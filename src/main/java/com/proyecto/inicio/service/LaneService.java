package com.proyecto.inicio.service;

import com.proyecto.inicio.dto.request.LaneRequestDto;
import com.proyecto.inicio.dto.response.LaneResponseDto;
import com.proyecto.inicio.entity.Lane;
import com.proyecto.inicio.entity.Pool;
import com.proyecto.inicio.entity.RolProceso;
import com.proyecto.inicio.repository.LaneRepository;
import com.proyecto.inicio.repository.PoolRepository;
import com.proyecto.inicio.repository.RolProcesoRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.Objects;
import java.util.HashSet;
import com.proyecto.inicio.repository.ActividadRepository;
import com.proyecto.inicio.dto.DiagramaDto.*;
import com.proyecto.inicio.dto.ProcesoDto.Retirada;
import com.proyecto.inicio.entity.enums.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.validation.annotation.Validated;

@Service
@Validated
@RequiredArgsConstructor
public class LaneService {
    private final LaneRepository laneRepository;
    private final PoolRepository poolRepository;
    private final RolProcesoRepository rolProcesoRepository;
    private final ActividadRepository actividades;
    private final ContextoColaboracionService contexto;
    private final PermisoEstructuraService permisos;

    @Transactional
    public LaneVista crear(@NotNull @Positive Long actorId, @NotNull @Positive Long procesoId,
            @NotNull @Positive Long poolId, @NotNull @Valid LaneDatos datos) {
        permisos.exigir(actorId, procesoId, RecursoEstructura.LANE, AccionEstructura.CREAR);
        Pool pool = contexto.pool(poolId, procesoId);
        validarPool(pool);
        validarRol(pool, datos.rolProcesoId());
        if (laneRepository.listarTodas(poolId).stream().anyMatch(l -> Objects.equals(l.getOrden(), datos.orden())))
            throw new IllegalArgumentException("Ya existe una lane con ese orden.");
        var respuesta = crear(new LaneRequestDto(poolId, datos.rolProcesoId(), datos.orden(), datos.altura()));
        Lane lane = laneRepository.findById(respuesta.getId()).orElseThrow();
        contexto.auditar(actorId, pool.getProceso(), "LANE", lane.getId(), AccionHistorial.CREAR, "Creación de lane.");
        return vista(lane);
    }

    @Transactional
    public LaneVista actualizar(@NotNull @Positive Long actorId, @NotNull @Positive Long procesoId,
            @NotNull @Positive Long poolId, @NotNull @Positive Long laneId, @NotNull @Valid LaneDatos datos) {
        permisos.exigir(actorId, procesoId, RecursoEstructura.LANE, AccionEstructura.EDITAR);
        Pool pool = contexto.pool(poolId, procesoId); validarPool(pool);
        Lane lane = buscar(laneId, poolId);
        contexto.version(datos.version(), lane.getVersion());
        if (laneRepository.listarTodas(poolId).stream().anyMatch(l -> !l.getId().equals(laneId)
                && Objects.equals(l.getOrden(), datos.orden()))) throw new IllegalArgumentException("El orden ya está ocupado.");
        lane.setRolProceso(validarRol(pool, datos.rolProcesoId()));
        lane.setOrden(datos.orden()); lane.setAltura(datos.altura());
        laneRepository.saveAndFlush(lane);
        contexto.auditar(actorId, pool.getProceso(), "LANE", laneId, AccionHistorial.ACTUALIZAR, "Edición de lane.");
        return vista(lane);
    }

    @Transactional
    public void retirar(@NotNull @Positive Long actorId, @NotNull @Positive Long procesoId,
            @NotNull @Positive Long poolId, @NotNull @Positive Long laneId, @NotNull @Valid Retirada datos) {
        permisos.exigir(actorId, procesoId, RecursoEstructura.LANE, AccionEstructura.RETIRAR);
        Pool pool = contexto.pool(poolId, procesoId);
        Lane lane = buscar(laneId, poolId);
        contexto.version(datos.version(), lane.getVersion());
        contexto.confirmar(Boolean.TRUE.equals(datos.confirmar()));
        retirar(laneId, poolId);
        contexto.auditar(actorId, pool.getProceso(), "LANE", laneId, AccionHistorial.DESACTIVAR, "Retirada lógica de lane.");
    }

    @Transactional
    public List<LaneVista> reordenar(@NotNull @Positive Long actorId, @NotNull @Positive Long procesoId,
            @NotNull @Positive Long poolId, @NotNull @Valid Reordenar datos) {
        permisos.exigir(actorId, procesoId, RecursoEstructura.LANE, AccionEstructura.EDITAR);
        Pool pool = contexto.pool(poolId, procesoId); validarPool(pool);
        List<Lane> actuales = laneRepository.findByPoolIdAndActivoTrueOrderByOrden(poolId);
        Map<Long, Lane> porId = new LinkedHashMap<>();
        actuales.forEach(l -> porId.put(l.getId(), l));
        var ids = datos.lanes().stream().map(OrdenLane::id).toList();
        if (ids.size() != new HashSet<>(ids).size() || !new HashSet<>(ids).equals(porId.keySet()))
            throw new IllegalArgumentException("Indica todas las lanes activas del pool, sin repetir.");
        datos.lanes().forEach(d -> contexto.version(d.version(), porId.get(d.id()).getVersion()));
        List<Integer> posiciones = actuales.stream().map(Lane::getOrden).toList();
        int temporal = laneRepository.listarTodas(poolId).stream().mapToInt(Lane::getOrden).max().orElse(0) + 1;
        // Se usan posiciones temporales para no chocar con la restricción de orden único.
        for (Lane lane : actuales) { lane.setOrden(temporal++); laneRepository.saveAndFlush(lane); }
        for (int i = 0; i < ids.size(); i++) {
            Lane lane = porId.get(ids.get(i)); lane.setOrden(posiciones.get(i)); laneRepository.saveAndFlush(lane);
        }
        contexto.auditar(actorId, pool.getProceso(), "POOL", poolId, AccionHistorial.ACTUALIZAR, "Reordenamiento de lanes.");
        return laneRepository.findByPoolIdAndActivoTrueOrderByOrden(poolId).stream().map(this::vista).toList();
    }

    @Transactional(readOnly = true)
    public List<LaneVista> listar(@NotNull @Positive Long actorId, @NotNull @Positive Long procesoId,
            @NotNull @Positive Long poolId) {
        contexto.proceso(actorId, procesoId, false, false, true);
        contexto.pool(poolId, procesoId);
        return laneRepository.findByPoolIdAndActivoTrueOrderByOrden(poolId).stream().map(this::vista).toList();
    }

    private void validarPool(Pool pool) {
        if (Boolean.TRUE.equals(pool.getCajaNegra())) throw new IllegalArgumentException("Una caja negra no tiene lanes.");
    }

    private RolProceso validarRol(Pool pool, Long rolId) {
        return rolProcesoRepository.findByIdAndEmpresaIdAndActivoTrue(rolId, pool.getProceso().getEmpresa().getId())
                .orElseThrow(() -> new EntityNotFoundException("Rol activo no encontrado en la empresa."));
    }

    private Lane buscar(Long id, Long poolId) {
        return laneRepository.findByIdAndPoolIdAndActivoTrue(id, poolId)
                .orElseThrow(() -> new EntityNotFoundException("Lane no encontrada en el pool."));
    }

    private LaneVista vista(Lane l) {
        return new LaneVista(l.getId(), l.getPool().getId(), l.getRolProceso().getId(),
                l.getRolProceso().getNombre(), l.getOrden(), l.getAltura(), l.getVersion());
    }

    @Transactional
    public LaneResponseDto crear(LaneRequestDto request) {
        Pool pool = poolRepository.findById(request.getPoolId())
                .orElseThrow(() -> new EntityNotFoundException("Pool no encontrado"));
        validarPool(pool);
        RolProceso rol = rolProcesoRepository.findById(request.getRolProcesoId())
                .orElseThrow(() -> new EntityNotFoundException("Rol de proceso no encontrado"));
        if (!pool.getProceso().getEmpresa().getId().equals(rol.getEmpresa().getId()))
            throw new IllegalArgumentException("La lane y su rol deben pertenecer a la misma empresa");
        Lane lane = Lane.builder().pool(pool).rolProceso(rol).orden(request.getOrden())
                .altura(request.getAltura()).activo(true).build();
        return convertir(laneRepository.save(lane));
    }

    @Transactional(readOnly = true)
    public List<LaneResponseDto> listarPorPool(Long poolId) {
        return laneRepository.findByPoolIdAndActivoTrueOrderByOrden(poolId).stream().map(this::convertir).toList();
    }

    @Transactional
    public void retirar(Long laneId, Long poolId) {
        Lane lane = laneRepository.findByIdAndPoolIdAndActivoTrue(laneId, poolId)
                .orElseThrow(() -> new EntityNotFoundException("Lane no encontrada en el pool"));
        if (actividades.laneEnUso(laneId))
            throw new IllegalStateException("Reasigna las actividades antes de retirar esta lane.");
        lane.setActivo(false);
        laneRepository.save(lane);
    }

    private LaneResponseDto convertir(Lane lane) {
        return new LaneResponseDto(lane.getId(), lane.getPool().getId(), lane.getRolProceso().getId(),
                lane.getRolProceso().getNombre(), lane.getOrden(), lane.getAltura(), lane.getActivo());
    }
}
