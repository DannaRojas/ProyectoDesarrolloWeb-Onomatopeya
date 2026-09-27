package com.proyecto.inicio.controller.mensajeria;

import com.proyecto.inicio.dto.request.UsoMensajeActividadRequestDto;
import com.proyecto.inicio.dto.response.UsoMensajeActividadResponseDto;
import com.proyecto.inicio.service.UsoMensajeActividadService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/procesos/{procesoId}/mensajes/{mensajeId}/usos")
@Profile("conexion-empresa")
@RequiredArgsConstructor
public class UsoMensajeActividadController {
    private final UsoMensajeActividadService servicio;
    private final UsuarioSesionColaboracion sesion;

    @PostMapping
    public ResponseEntity<UsoMensajeActividadResponseDto> crear(Principal principal, @PathVariable @Positive Long procesoId, @PathVariable @Positive Long mensajeId,
            @Valid @RequestBody UsoMensajeActividadRequestDto datos) {
        return ResponseEntity.status(201).body(servicio.crear(sesion.id(principal), procesoId, mensajeId, datos));
    }

    @GetMapping
    public List<UsoMensajeActividadResponseDto> listar(Principal principal, @PathVariable @Positive Long procesoId, @PathVariable @Positive Long mensajeId) {
        return servicio.listar(sesion.id(principal), procesoId, mensajeId);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> retirar(Principal principal, @PathVariable @Positive Long procesoId, @PathVariable @Positive Long mensajeId,
            @PathVariable @Positive Long id, @RequestParam @PositiveOrZero Long version,
            @RequestParam(defaultValue = "false") boolean confirmar) {
        servicio.retirar(sesion.id(principal), procesoId, mensajeId, id, version, confirmar);
        return ResponseEntity.noContent().build();
    }
}
