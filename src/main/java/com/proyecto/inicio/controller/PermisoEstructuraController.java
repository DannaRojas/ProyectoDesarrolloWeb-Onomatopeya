package com.proyecto.inicio.controller;

import com.proyecto.inicio.dto.PermisoEstructuraDto.PermisoEstructuraRequestDto;
import com.proyecto.inicio.dto.PermisoEstructuraDto.PermisoEstructuraResponseDto;
import com.proyecto.inicio.entity.enums.AccionEstructura;
import com.proyecto.inicio.entity.enums.RecursoEstructura;
import com.proyecto.inicio.service.PermisoEstructuraService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.*;
import com.proyecto.inicio.service.UsuarioSesionService;
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

    @PutMapping
    public PermisoEstructuraResponseDto guardar(Principal principal, @PathVariable @Positive Long procesoId,
            @Valid @RequestBody PermisoEstructuraRequestDto datos) {
        return servicio.guardar(sesion.id(principal), procesoId, datos);
    }

    @GetMapping
    public List<PermisoEstructuraResponseDto> listar(Principal principal, @PathVariable @Positive Long procesoId) {
        return servicio.listar(sesion.id(principal), procesoId);
    }

    @GetMapping("/verificar")
    public Map<String, Boolean> verificar(Principal principal, @PathVariable @Positive Long procesoId,
            @RequestParam RecursoEstructura recurso, @RequestParam AccionEstructura accion) {
        return Map.of("permitido", servicio.permitido(sesion.id(principal), procesoId, recurso, accion));
    }
}
