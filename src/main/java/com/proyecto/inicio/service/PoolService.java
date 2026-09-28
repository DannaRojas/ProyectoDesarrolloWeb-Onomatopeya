package com.proyecto.inicio.service;

import com.proyecto.inicio.dto.request.PoolRequestDto;
import com.proyecto.inicio.dto.response.PoolResponseDto;
import com.proyecto.inicio.entity.Pool;
import com.proyecto.inicio.entity.Proceso;
import com.proyecto.inicio.repository.PoolRepository;
import com.proyecto.inicio.repository.ProcesoRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.proyecto.inicio.repository.LaneRepository;
import com.proyecto.inicio.repository.NodoRepository;
import com.proyecto.inicio.repository.FlujoMensajeRepository;
import com.proyecto.inicio.entity.enums.*;
import com.proyecto.inicio.dto.DiagramaDto.*;
import com.proyecto.inicio.dto.ProcesoDto.Retirada;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.validation.annotation.Validated;
import java.util.List;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;

@Service
@Validated
@RequiredArgsConstructor
public class PoolService {

    private final PoolRepository poolRepository;
    private final ProcesoRepository procesoRepository;
    private final LaneRepository laneRepository;
    private final NodoRepository nodos;
    private final FlujoMensajeRepository flujos;
    private final ContextoColaboracionService contexto;
    private final PermisoEstructuraService permisos;

    @Transactional
    public PoolVista crear(@NotNull @Positive Long actorId, @NotNull @Positive Long procesoId,
            @NotNull @Valid PoolDatos datos) {
        permisos.exigir(actorId, procesoId, RecursoEstructura.POOL, AccionEstructura.CREAR);
        if (!Boolean.TRUE.equals(datos.cajaNegra()))
            throw new IllegalArgumentException("El pool externo debe ser una caja negra.");
        var respuesta = crearExterno(new PoolRequestDto(procesoId, datos.nombre().strip(), datos.tipoParticipante(),
                true, datos.posicionX(), datos.posicionY(), datos.ancho(), datos.alto(), datos.orden()));
        Pool pool = poolRepository.findById(respuesta.getId()).orElseThrow();
        contexto.auditar(actorId, pool.getProceso(), "POOL", pool.getId(), AccionHistorial.CREAR, "Creación de pool externo.");
        return vista(pool);
    }

    @Transactional
    public PoolVista actualizar(@NotNull @Positive Long actorId, @NotNull @Positive Long procesoId,
            @NotNull @Positive Long poolId, @NotNull @Valid PoolDatos datos) {
        permisos.exigir(actorId, procesoId, RecursoEstructura.POOL, AccionEstructura.EDITAR);
        Pool pool = contexto.pool(poolId, procesoId);
        contexto.version(datos.version(), pool.getVersion());
        if (Boolean.TRUE.equals(pool.getPropietario()) && (Boolean.TRUE.equals(datos.cajaNegra())
                || datos.tipoParticipante() != TipoParticipante.EMPRESA))
            throw new IllegalArgumentException("El pool propietario debe representar una empresa abierta.");
        if (!Boolean.TRUE.equals(pool.getPropietario()) && !Boolean.TRUE.equals(datos.cajaNegra()))
            throw new IllegalArgumentException("El pool externo debe ser una caja negra.");
        if (Boolean.TRUE.equals(datos.cajaNegra()) && (nodos.poolEnUso(poolId)
                || laneRepository.existsByPoolIdAndActivoTrue(poolId)))
            throw new IllegalStateException("Un pool con elementos internos no puede convertirse en caja negra.");
        pool.setNombre(datos.nombre().strip()); pool.setTipoParticipante(datos.tipoParticipante());
        pool.setCajaNegra(datos.cajaNegra()); pool.setPosicionX(datos.posicionX()); pool.setPosicionY(datos.posicionY());
        pool.setAncho(datos.ancho()); pool.setAlto(datos.alto()); pool.setOrden(datos.orden());
        poolRepository.saveAndFlush(pool);
        contexto.auditar(actorId, pool.getProceso(), "POOL", poolId, AccionHistorial.ACTUALIZAR, "Edición de pool.");
        return vista(pool);
    }

    @Transactional
    public void retirar(@NotNull @Positive Long actorId, @NotNull @Positive Long procesoId,
            @NotNull @Positive Long poolId, @NotNull @Valid Retirada datos) {
        permisos.exigir(actorId, procesoId, RecursoEstructura.POOL, AccionEstructura.RETIRAR);
        Pool pool = contexto.pool(poolId, procesoId);
        contexto.version(datos.version(), pool.getVersion());
        contexto.confirmar(Boolean.TRUE.equals(datos.confirmar()));
        if (nodos.poolEnUso(poolId) || flujos.listarActivos(procesoId).stream()
                .anyMatch(f -> f.getPoolOrigen().getId().equals(poolId) || f.getPoolDestino().getId().equals(poolId)))
            throw new IllegalStateException("El pool tiene nodos o flujos de mensaje activos.");
        retirar(poolId, procesoId);
        contexto.auditar(actorId, pool.getProceso(), "POOL", poolId, AccionHistorial.DESACTIVAR, "Retirada lógica de pool.");
    }

    @Transactional(readOnly = true)
    public List<PoolVista> listar(@NotNull @Positive Long actorId, @NotNull @Positive Long procesoId) {
        contexto.proceso(actorId, procesoId, false, false, true);
        return poolRepository.findByProcesoIdAndActivoTrue(procesoId).stream()
                .sorted(Comparator.comparing(Pool::getOrden).thenComparing(Pool::getId)).map(this::vista).toList();
    }

    @Transactional
    public List<PoolVista> reordenar(@NotNull @Positive Long actorId, @NotNull @Positive Long procesoId,
            @NotNull @Valid ReordenarPools datos) {
        permisos.exigir(actorId, procesoId, RecursoEstructura.POOL, AccionEstructura.EDITAR);
        var porId = new LinkedHashMap<Long, Pool>();
        poolRepository.findByProcesoIdAndActivoTrue(procesoId).forEach(p -> porId.put(p.getId(), p));
        var ids = datos.pools().stream().map(OrdenPool::id).toList();
        if (ids.size() != new HashSet<>(ids).size() || !new HashSet<>(ids).equals(porId.keySet()))
            throw new IllegalArgumentException("Indica todos los pools activos, sin repetir.");
        datos.pools().forEach(p -> contexto.version(p.version(), porId.get(p.id()).getVersion()));
        for (int i = 0; i < ids.size(); i++) porId.get(ids.get(i)).setOrden(i + 1);
        poolRepository.flush();
        Proceso proceso = porId.values().iterator().next().getProceso();
        contexto.auditar(actorId, proceso, "PROCESO", procesoId, AccionHistorial.ACTUALIZAR, "Reordenamiento de pools.");
        return listar(actorId, procesoId);
    }

    private PoolVista vista(Pool p) {
        return new PoolVista(p.getId(), p.getNombre(), p.getTipoParticipante(), p.getPropietario(), p.getCajaNegra(),
                p.getPosicionX(), p.getPosicionY(), p.getAncho(), p.getAlto(), p.getOrden(), p.getVersion());
    }

    @Transactional
    public PoolResponseDto crearExterno(PoolRequestDto request) {

        Proceso proceso = procesoRepository.findById(request.getProcesoId())
                .orElseThrow(() -> new EntityNotFoundException("Proceso no encontrado"));

        Pool pool = Pool.builder()
                .proceso(proceso)
                .nombre(request.getNombre())
                .tipoParticipante(request.getTipoParticipante())
                .propietario(false)
                .cajaNegra(request.getCajaNegra())
                .posicionX(request.getPosicionX())
                .posicionY(request.getPosicionY())
                .ancho(request.getAncho())
                .alto(request.getAlto())
                .orden(request.getOrden())
                .activo(true)
                .build();

        return convertirAResponse(poolRepository.save(pool));
    }

    @Transactional(readOnly = true)
    public List<PoolResponseDto> listarPorProceso(Long procesoId) {
        return poolRepository.findByProcesoIdAndActivoTrue(procesoId)
                .stream()
                .map(this::convertirAResponse)
                .toList();
    }
    @Transactional
public void retirar(Long poolId, Long procesoId) {

    Pool pool = poolRepository
            .findByIdAndProcesoIdAndActivoTrue(poolId, procesoId)
            .orElseThrow(() -> new EntityNotFoundException(
                    "Pool no encontrado o no pertenece al proceso"));

    if (Boolean.TRUE.equals(pool.getPropietario())) {
        throw new IllegalStateException(
                "No se puede retirar el pool propietario del proceso");
    }

    if (laneRepository.existsByPoolIdAndActivoTrue(poolId)) {
        throw new IllegalStateException(
                "No se puede retirar el pool porque tiene lanes activas");
    }

    pool.setActivo(false);
    poolRepository.save(pool);
}

    private PoolResponseDto convertirAResponse(Pool pool) {
        return new PoolResponseDto(
                pool.getId(),
                pool.getProceso().getId(),
                pool.getNombre(),
                pool.getTipoParticipante(),
                pool.getPropietario(),
                pool.getCajaNegra(),
                pool.getPosicionX(),
                pool.getPosicionY(),
                pool.getAncho(),
                pool.getAlto(),
                pool.getOrden(),
                pool.getActivo()
        );
    }
}    

