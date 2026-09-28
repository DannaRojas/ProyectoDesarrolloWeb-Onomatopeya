package com.proyecto.inicio.service;

import com.proyecto.inicio.dto.request.RolProcesoRequestDto;
import com.proyecto.inicio.dto.response.RolProcesoResponseDto;
import com.proyecto.inicio.entity.Empresa;
import com.proyecto.inicio.entity.RolProceso;
import com.proyecto.inicio.repository.EmpresaRepository;
import com.proyecto.inicio.repository.RolProcesoRepository;
import com.proyecto.inicio.repository.LaneRepository;
import com.proyecto.inicio.repository.ActividadRepository;
import com.proyecto.inicio.repository.HistorialCambioRepository;
import com.proyecto.inicio.entity.HistorialCambio;
import com.proyecto.inicio.entity.Usuario;
import com.proyecto.inicio.entity.enums.*;
import com.proyecto.inicio.dto.DiagramaDto.*;
import com.proyecto.inicio.dto.ProcesoDto.Retirada;
import com.proyecto.inicio.exception.AccesoColaboracionException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.validation.annotation.Validated;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Validated
@RequiredArgsConstructor
public class RolProcesoService {

    private final RolProcesoRepository rolProcesoRepository;
    private final EmpresaRepository empresaRepository;
    private final LaneRepository lanes;
    private final ActividadRepository actividades;
    private final HistorialCambioRepository historial;
    private final ContextoColaboracionService contexto;

    @Transactional(readOnly = true)
    public PaginaRoles buscar(@NotNull @Positive Long actorId, String nombre, @Min(0) int pagina,
            @Min(1) @Max(100) int tamanio) {
        Usuario actor = contexto.usuario(actorId);
        var resultado = rolProcesoRepository.buscar(actor.getEmpresa().getId(), nombre == null ? "" : nombre.strip(),
                PageRequest.of(pagina, tamanio, Sort.by("nombre", "id")));
        return new PaginaRoles(resultado.map(this::vista).getContent(), pagina, tamanio,
                resultado.getTotalElements(), resultado.getTotalPages());
    }

    @Transactional
    public RolVista crear(@NotNull @Positive Long actorId, @NotNull @Valid RolDatos datos) {
        Usuario admin = administrador(actorId);
        if (rolProcesoRepository.nombreOcupado(admin.getEmpresa().getId(), datos.nombre().strip(), null))
            throw new IllegalArgumentException("Ya existe un rol con ese nombre en la empresa.");
        RolProceso rol = rolProcesoRepository.saveAndFlush(RolProceso.builder().empresa(admin.getEmpresa())
                .nombre(datos.nombre().strip()).descripcion(datos.descripcion()).activo(true).build());
        auditar(admin, rol, AccionHistorial.CREAR);
        return vista(rol);
    }

    @Transactional
    public RolVista actualizar(@NotNull @Positive Long actorId, @NotNull @Positive Long rolId,
            @NotNull @Valid RolDatos datos) {
        Usuario admin = administrador(actorId);
        RolProceso rol = buscarRol(admin, rolId);
        contexto.version(datos.version(), rol.getVersion());
        if (rolProcesoRepository.nombreOcupado(admin.getEmpresa().getId(), datos.nombre().strip(), rolId))
            throw new IllegalArgumentException("Ya existe un rol con ese nombre en la empresa.");
        rol.setNombre(datos.nombre().strip());
        rol.setDescripcion(datos.descripcion());
        rolProcesoRepository.saveAndFlush(rol);
        auditar(admin, rol, AccionHistorial.ACTUALIZAR);
        return vista(rol);
    }

    @Transactional
    public void retirar(@NotNull @Positive Long actorId, @NotNull @Positive Long rolId,
            @NotNull @Valid Retirada datos) {
        Usuario admin = administrador(actorId);
        RolProceso rol = buscarRol(admin, rolId);
        contexto.version(datos.version(), rol.getVersion());
        contexto.confirmar(Boolean.TRUE.equals(datos.confirmar()));
        if (lanes.existsByRolProcesoIdAndActivoTrue(rolId) || actividades.rolEnUso(rolId))
            throw new IllegalStateException("El rol está en uso en los procesos " + lanes.procesosEnUso(rolId));
        rol.setActivo(false);
        rolProcesoRepository.saveAndFlush(rol);
        auditar(admin, rol, AccionHistorial.DESACTIVAR);
    }

    private Usuario administrador(Long actorId) {
        Usuario admin = contexto.usuario(actorId);
        if (admin.getRolAcceso() != RolAcceso.ADMINISTRADOR) throw new AccesoColaboracionException();
        return admin;
    }

    private RolProceso buscarRol(Usuario admin, Long id) {
        return rolProcesoRepository.findByIdAndEmpresaIdAndActivoTrue(id, admin.getEmpresa().getId())
                .orElseThrow(() -> new EntityNotFoundException("Rol no encontrado en la empresa."));
    }

    private RolVista vista(RolProceso rol) {
        return new RolVista(rol.getId(), rol.getNombre(), rol.getDescripcion(), rol.getActivo(),
                rol.getVersion(), lanes.procesosEnUso(rol.getId()));
    }

    private void auditar(Usuario actor, RolProceso rol, AccionHistorial accion) {
        historial.save(HistorialCambio.builder().empresaContexto(actor.getEmpresa()).actor(actor)
                .tipoActor(TipoActor.USUARIO).tipoObjeto("ROL_PROCESO").objetoId(rol.getId())
                .accion(accion).descripcion("Cambio en rol de proceso: " + rol.getNombre()).build());
        for (Long procesoId : lanes.procesosEnUso(rol.getId())) {
            var proceso = contexto.proceso(actor.getId(), procesoId, false, true, true);
            contexto.auditar(actor.getId(), proceso, "ROL_PROCESO", rol.getId(), accion, "Actualización del rol de una lane.");
        }
    }

    @Transactional
    public RolProcesoResponseDto crear(RolProcesoRequestDto request) {

        Empresa empresa = empresaRepository.findById(request.getEmpresaId())
                .orElseThrow(() -> new EntityNotFoundException("Empresa no encontrada"));

        if (rolProcesoRepository.existsByEmpresaIdAndNombre(
                empresa.getId(), request.getNombre())) {
            throw new IllegalArgumentException(
                    "Ya existe un rol de proceso con ese nombre en la empresa");
        }

        RolProceso rolProceso = RolProceso.builder()
                .empresa(empresa)
                .nombre(request.getNombre())
                .descripcion(request.getDescripcion())
                .activo(true)
                .build();

        RolProceso rolGuardado = rolProcesoRepository.save(rolProceso);

        return convertirAResponse(rolGuardado);
    }

    @Transactional(readOnly = true)
    public List<RolProcesoResponseDto> listarPorEmpresa(Long empresaId) {
        return rolProcesoRepository.findByEmpresaIdAndActivoTrue(empresaId)
                .stream()
                .map(this::convertirAResponse)
                .toList();
    }

    private RolProcesoResponseDto convertirAResponse(RolProceso rolProceso) {
        return new RolProcesoResponseDto(
                rolProceso.getId(),
                rolProceso.getEmpresa().getId(),
                rolProceso.getNombre(),
                rolProceso.getDescripcion(),
                rolProceso.getActivo()
        );
    }
}
