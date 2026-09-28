package com.proyecto.inicio.service;

import com.proyecto.inicio.dto.request.ProcesoRequestDto;
import com.proyecto.inicio.dto.response.ProcesoResponseDto;
import com.proyecto.inicio.entity.Empresa;
import com.proyecto.inicio.entity.Pool;
import com.proyecto.inicio.entity.Proceso;
import com.proyecto.inicio.entity.Usuario;
import com.proyecto.inicio.entity.enums.EstadoPublicacion;
import com.proyecto.inicio.entity.enums.TipoParticipante;
import com.proyecto.inicio.repository.EmpresaRepository;
import com.proyecto.inicio.repository.PoolRepository;
import com.proyecto.inicio.repository.ProcesoRepository;
import com.proyecto.inicio.repository.UsuarioRepository;
import com.proyecto.inicio.repository.HistorialCambioRepository;
import com.proyecto.inicio.repository.AccesoProcesoRepository;
import com.proyecto.inicio.entity.AccesoProceso;
import com.proyecto.inicio.entity.enums.*;
import com.proyecto.inicio.dto.ProcesoDto.*;
import com.proyecto.inicio.exception.AccesoColaboracionException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.validation.annotation.Validated;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.time.OffsetDateTime;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

@Service
@Validated
@RequiredArgsConstructor
public class ProcesoService {

    private final ProcesoRepository procesoRepository;
    private final PoolRepository poolRepository;
    private final EmpresaRepository empresaRepository;
    private final UsuarioRepository usuarioRepository;
    private final ContextoColaboracionService contexto;
    private final HistorialCambioRepository historial;
    private final AccesoProcesoRepository accesos;
    private final NodoService nodos;
    private final MensajeService mensajes;

    @Transactional
    public ProcesoResponseDto crear(@NotNull ProcesoRequestDto request) {

        Usuario actor = contexto.usuario(request.getCreadorId());
        if (actor.getRolAcceso() == RolAcceso.LECTURA) throw new AccesoColaboracionException();
        if (request.getNombre() == null || request.getNombre().isBlank() || request.getNombre().strip().length() > 150
                || request.getCategoria() != null && request.getCategoria().length() > 100) {
            throw new IllegalArgumentException("Revisa el nombre y la categoría del proceso.");
        }

        Empresa empresa = empresaRepository.findById(request.getEmpresaId())
                .orElseThrow(() -> new EntityNotFoundException("Empresa no encontrada"));

        Usuario creador = usuarioRepository.findById(request.getCreadorId())
                .orElseThrow(() -> new EntityNotFoundException("Usuario creador no encontrado"));

        if (!Objects.equals(creador.getEmpresa().getId(), empresa.getId())) {
            throw new IllegalArgumentException(
                    "El creador debe pertenecer a la empresa del proceso");
        }

        if (procesoRepository.nombreOcupado(
                empresa.getId(), request.getNombre().strip(), null)) {
            throw new IllegalArgumentException(
                    "Ya existe un proceso con ese nombre en la empresa");
        }

        Proceso proceso = Proceso.builder()
                .empresa(empresa)
                .creador(creador)
                .nombre(request.getNombre().strip())
                .descripcion(request.getDescripcion())
                .categoria(request.getCategoria())
                .estadoPublicacion(EstadoPublicacion.BORRADOR)
                .activo(true)
                .build();

        Proceso procesoGuardado = procesoRepository.save(proceso);

        Pool poolPropietario = Pool.builder()
                .proceso(procesoGuardado)
                .nombre(empresa.getNombre())
                .tipoParticipante(TipoParticipante.EMPRESA)
                .propietario(true)
                .cajaNegra(false)
                .posicionX(0)
                .posicionY(0)
                .ancho(1000)
                .alto(600)
                .orden(1)
                .activo(true)
                .build();

        poolRepository.save(poolPropietario);
        contexto.auditar(creador.getId(), procesoGuardado, "PROCESO", procesoGuardado.getId(),
                AccionHistorial.CREAR, "Creación de proceso.");

        return convertirAResponse(procesoGuardado);
    }

    @Transactional(readOnly = true)
    public List<ProcesoResponseDto> listarPorEmpresa(Long empresaId) {
        return procesoRepository.findByEmpresaIdAndActivoTrue(empresaId)
                .stream()
                .map(this::convertirAResponse)
                .toList();
    }

    @Transactional
    public void retirar(Long procesoId, Long empresaId) {
        Proceso proceso = procesoRepository
                .findByIdAndEmpresaIdAndActivoTrue(procesoId, empresaId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Proceso no encontrado o no pertenece a la empresa"));

        proceso.setActivo(false);
        procesoRepository.save(proceso);
    }

    private ProcesoResponseDto convertirAResponse(Proceso proceso) {
        return new ProcesoResponseDto(
                proceso.getId(),
                proceso.getEmpresa().getId(),
                proceso.getCreador().getId(),
                proceso.getNombre(),
                proceso.getDescripcion(),
                proceso.getCategoria(),
                proceso.getEstadoPublicacion(),
                proceso.getActivo(),
                proceso.getFechaCreacion()
        );
    }

    @Transactional(readOnly = true)
    public Pagina buscar(@NotNull @Positive Long actorId, String nombre, String categoria,
            EstadoPublicacion estado, boolean activo, @Min(0) int pagina, @Min(1) @Max(100) int tamanio) {
        Usuario actor = contexto.usuario(actorId);
        var resultado = procesoRepository.buscar(actor.getEmpresa().getId(),
                nombre == null ? "" : nombre.strip(), categoria == null ? "" : categoria.strip(),
                estado, activo, PageRequest.of(pagina, tamanio, Sort.by("nombre", "id")));
        return new Pagina(resultado.map(this::ficha).getContent(), pagina, tamanio,
                resultado.getTotalElements(), resultado.getTotalPages());
    }

    @Transactional(readOnly = true)
    public Ficha consultar(@NotNull @Positive Long actorId, @NotNull @Positive Long procesoId) {
        return ficha(contexto.proceso(actorId, procesoId, false, false, true));
    }

    @Transactional
    public Ficha crear(@NotNull @Positive Long actorId, @NotNull @Valid Datos datos) {
        if (datos.estadoPublicacion() != EstadoPublicacion.BORRADOR)
            throw new IllegalArgumentException("El proceso debe crearse como borrador.");
        Usuario actor = contexto.usuario(actorId);
        var respuesta = crear(new ProcesoRequestDto(actor.getEmpresa().getId(), actorId,
                datos.nombre(), datos.descripcion(), datos.categoria()));
        return ficha(procesoRepository.findById(respuesta.getId()).orElseThrow());
    }

    @Transactional
    public Ficha actualizar(@NotNull @Positive Long actorId, @NotNull @Positive Long procesoId,
            @NotNull @Valid Datos datos) {
        Proceso proceso = contexto.proceso(actorId, procesoId, true, false);
        contexto.version(datos.version(), proceso.getVersion());
        if (procesoRepository.nombreOcupado(proceso.getEmpresa().getId(), datos.nombre().strip(), procesoId))
            throw new IllegalArgumentException("Ya existe un proceso con ese nombre en la empresa.");
        if (datos.estadoPublicacion() == EstadoPublicacion.PUBLICADO) {
            List<String> avisos = new java.util.ArrayList<>(nodos.advertencias(actorId, procesoId));
            avisos.addAll(mensajes.advertencias(actorId, procesoId));
            if (!avisos.isEmpty()) throw new IllegalStateException("Revisa el diagrama antes de publicar: " + String.join("; ", avisos));
        }
        proceso.setNombre(datos.nombre().strip());
        proceso.setDescripcion(datos.descripcion());
        proceso.setCategoria(datos.categoria());
        proceso.setEstadoPublicacion(datos.estadoPublicacion());
        procesoRepository.saveAndFlush(proceso);
        contexto.auditar(actorId, proceso, "PROCESO", procesoId, AccionHistorial.ACTUALIZAR, "Edición de proceso.");
        return ficha(proceso);
    }

    @Transactional
    public void retirar(@NotNull @Positive Long actorId, @NotNull @Positive Long procesoId,
            @NotNull @Valid Retirada datos) {
        Proceso proceso = contexto.proceso(actorId, procesoId, true, true);
        contexto.version(datos.version(), proceso.getVersion());
        contexto.confirmar(Boolean.TRUE.equals(datos.confirmar()));
        proceso.setActivo(false);
        procesoRepository.saveAndFlush(proceso);
        contexto.auditar(actorId, proceso, "PROCESO", procesoId, AccionHistorial.DESACTIVAR, "Retirada lógica de proceso.");
    }

    @Transactional(readOnly = true)
    public List<Cambio> historial(@NotNull @Positive Long actorId, @NotNull @Positive Long procesoId) {
        contexto.proceso(actorId, procesoId, false, false, true);
        return historial.listarPorProceso(procesoId).stream().map(h -> new Cambio(h.getId(),
                h.getActor() == null ? null : h.getActor().getId(), h.getFecha(), h.getTipoObjeto(),
                h.getObjetoId(), h.getAccion(), h.getDescripcion())).toList();
    }

    @Transactional
    public Acceso compartir(@NotNull @Positive Long actorId, @NotNull @Positive Long procesoId,
            @NotNull @Valid Compartir datos) {
        Proceso proceso = contexto.proceso(actorId, procesoId, true, true);
        contexto.version(datos.version(), proceso.getVersion());
        if (datos.empresaId().equals(proceso.getEmpresa().getId()))
            throw new IllegalArgumentException("La empresa propietaria ya tiene acceso.");
        Empresa invitada = empresaRepository.findById(datos.empresaId()).filter(e -> Boolean.TRUE.equals(e.getActivo()))
                .orElseThrow(() -> new EntityNotFoundException("Empresa invitada no encontrada."));
        AccesoProceso acceso = accesos.findByProcesoIdAndEmpresaInvitadaId(procesoId, invitada.getId())
                .orElseGet(() -> AccesoProceso.builder().proceso(proceso).empresaInvitada(invitada).build());
        if (Boolean.TRUE.equals(datos.permitido())) {
            acceso.setActivo(true);
            acceso.setOtorgadoPor(contexto.usuario(actorId));
            acceso.setOtorgadoEn(OffsetDateTime.now());
            acceso.setRevocadoPor(null);
            acceso.setRevocadoEn(null);
        } else {
            if (acceso.getId() == null) throw new IllegalArgumentException("La empresa no tiene acceso compartido.");
            acceso.setActivo(false);
            acceso.setRevocadoPor(contexto.usuario(actorId));
            acceso.setRevocadoEn(OffsetDateTime.now());
        }
        accesos.saveAndFlush(acceso);
        contexto.auditar(actorId, proceso, "ACCESO_PROCESO", acceso.getId(), AccionHistorial.COMPARTIR,
                Boolean.TRUE.equals(datos.permitido()) ? "Consulta compartida habilitada." : "Consulta compartida revocada.");
        return new Acceso(acceso.getId(), procesoId, invitada.getId(), acceso.getActivo(), acceso.getVersion());
    }

    private Ficha ficha(Proceso p) {
        return new Ficha(p.getId(), p.getEmpresa().getId(), p.getCreador().getId(), p.getNombre(),
                p.getDescripcion(), p.getCategoria(), p.getEstadoPublicacion(), p.getActivo(), p.getFechaCreacion(), p.getVersion());
    }
}
