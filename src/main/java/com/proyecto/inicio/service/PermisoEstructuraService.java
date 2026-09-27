package com.proyecto.inicio.service;

import com.proyecto.inicio.dto.request.PermisoEstructuraRequestDto;
import com.proyecto.inicio.dto.response.PermisoEstructuraResponseDto;
import com.proyecto.inicio.entity.*;
import com.proyecto.inicio.entity.enums.*;
import com.proyecto.inicio.exception.AccesoColaboracionException;
import com.proyecto.inicio.repository.PermisoEstructuraRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.util.List;
import java.util.Objects;

@Service @Validated @RequiredArgsConstructor
@Transactional
public class PermisoEstructuraService {
    private final PermisoEstructuraRepository permisos;
    private final ContextoColaboracionService contexto;

    public PermisoEstructuraResponseDto guardar(Long usuarioId, Long procesoId, @NotNull @Valid PermisoEstructuraRequestDto datos) {
        Proceso proceso = contexto.proceso(usuarioId, procesoId, true, true);
        if (datos.getRolAcceso() == RolAcceso.LECTURA && Boolean.TRUE.equals(datos.getPermitido()))
            throw new IllegalArgumentException("El rol de lectura nunca puede modificar la estructura.");
        if (datos.getRolAcceso() == RolAcceso.ADMINISTRADOR && !Boolean.TRUE.equals(datos.getPermitido()))
            throw new IllegalArgumentException("El administrador conserva la gestión de la estructura.");
        PermisoEstructura permiso = permisos.buscarRegla(procesoId, datos.getRolAcceso(), datos.getRecurso(), datos.getAccion())
                .orElseGet(PermisoEstructura::new);
        if (permiso.getId() != null) contexto.version(datos.getVersion(), permiso.getVersion());
        permiso.setProceso(proceso); permiso.setRolAcceso(datos.getRolAcceso());
        permiso.setRecurso(datos.getRecurso()); permiso.setAccion(datos.getAccion()); permiso.setPermitido(datos.getPermitido());
        permisos.saveAndFlush(permiso);
        contexto.auditar(usuarioId, proceso, "PERMISO_ESTRUCTURA", permiso.getId(), AccionHistorial.ACTUALIZAR,
                "Se configuró " + datos.getAccion() + " sobre " + datos.getRecurso() + " para " + datos.getRolAcceso());
        return respuesta(permiso);
    }

    @Transactional(readOnly = true)
    public List<PermisoEstructuraResponseDto> listar(Long usuarioId, Long procesoId) {
        // Compartir un diagrama no comparte la configuración interna de permisos.
        contexto.proceso(usuarioId, procesoId, false, true);
        return permisos.listarPorProceso(procesoId).stream().map(this::respuesta).toList();
    }

    @Transactional(readOnly = true)
    public boolean permitido(Long usuarioId, Long procesoId, RecursoEstructura recurso, AccionEstructura accion) {
        if (recurso == null || accion == null) throw new IllegalArgumentException("Indica recurso y acción.");
        Proceso proceso = contexto.proceso(usuarioId, procesoId, false, false);
        Usuario usuario = contexto.usuario(usuarioId);
        if (!Objects.equals(usuario.getEmpresa().getId(), proceso.getEmpresa().getId()) || usuario.getRolAcceso() == RolAcceso.LECTURA)
            return false;
        if (usuario.getRolAcceso() == RolAcceso.ADMINISTRADOR) return true;
        return permisos.buscarRegla(procesoId, usuario.getRolAcceso(), recurso, accion)
                .map(p -> Boolean.TRUE.equals(p.getPermitido())).orElse(false);
    }

    public void exigir(Long usuarioId, Long procesoId, RecursoEstructura recurso, AccionEstructura accion) {
        contexto.proceso(usuarioId, procesoId, true, false);
        if (!permitido(usuarioId, procesoId, recurso, accion)) throw new AccesoColaboracionException();
    }

    private PermisoEstructuraResponseDto respuesta(PermisoEstructura p) {
        return new PermisoEstructuraResponseDto(p.getId(), p.getProceso().getId(), p.getRolAcceso(), p.getRecurso(),
                p.getAccion(), p.getPermitido(), p.getVersion());
    }
}
