package com.proyecto.inicio.controller;

import com.proyecto.inicio.dto.request.PermisoEstructuraRequestDto;
import com.proyecto.inicio.dto.response.PermisoEstructuraResponseDto;
import com.proyecto.inicio.entity.enums.AccionEstructura;
import com.proyecto.inicio.entity.enums.RecursoEstructura;
import com.proyecto.inicio.service.PermisoEstructuraService;
import com.proyecto.inicio.service.UsuarioSesionService;

import io.swagger.v3.oas.annotations.Operation;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;

import lombok.RequiredArgsConstructor;

import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/procesos/{procesoId}/permisos-estructura")
@Profile("conexion-empresa")
@RequiredArgsConstructor
public class PermisoEstructuraController {
    private final PermisoEstructuraService servicio;
    private final UsuarioSesionService sesion;

    @Operation(summary = "Configurar un permiso de estructura", description = "Define si un rol puede realizar una acción sobre pools o lanes del proceso.")
    @PutMapping
    public PermisoEstructuraResponseDto guardar(
            Principal principal,
            @PathVariable
            @Positive Long procesoId,
            @Valid
            @RequestBody PermisoEstructuraRequestDto datos) {
        return servicio.guardar(sesion.id(principal), procesoId, datos);
    }

    @Operation(summary = "Listar permisos de estructura", description = "Consulta la configuración de permisos de estructura del proceso accesible.")
    @GetMapping
    public List<PermisoEstructuraResponseDto> listar(
            Principal principal, @PathVariable @Positive Long procesoId) {
        return servicio.listar(sesion.id(principal), procesoId);
    }

    @Operation(summary = "Verificar un permiso de estructura", description = "Indica si el usuario actual puede realizar la acción solicitada sobre el recurso indicado.")
    @GetMapping("/verificar")
    public Map<String, Boolean> verificar(
            Principal principal,
            @PathVariable
            @Positive Long procesoId,
            @RequestParam RecursoEstructura recurso,
            @RequestParam AccionEstructura accion) {
        return Map.of(
                "permitido", servicio.permitido(sesion.id(principal), procesoId, recurso, accion));
    }
}
