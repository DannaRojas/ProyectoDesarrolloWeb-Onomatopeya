package com.proyecto.inicio.controller;

import com.proyecto.inicio.dto.FlujoMensajeDto.FlujoMensajeRequestDto;
import com.proyecto.inicio.dto.FlujoMensajeDto.FlujoMensajeResponseDto;
import com.proyecto.inicio.service.FlujoMensajeService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.proyecto.inicio.service.UsuarioSesionService;
import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/procesos/{procesoId}/flujos-mensaje")
@Profile("conexion-empresa")
@RequiredArgsConstructor
public class FlujoMensajeController {
    private final FlujoMensajeService servicio;
    private final UsuarioSesionService sesion;

    @PostMapping
    public ResponseEntity<FlujoMensajeResponseDto> crear(Principal principal, @PathVariable @Positive Long procesoId,
            @Valid @RequestBody FlujoMensajeRequestDto datos) {
        return ResponseEntity.status(201).body(servicio.crear(sesion.id(principal), procesoId, datos));
    }

    @GetMapping
    public List<FlujoMensajeResponseDto> listar(Principal principal, @PathVariable @Positive Long procesoId) {
        return servicio.listar(sesion.id(principal), procesoId);
    }

    @PutMapping("/{id}")
    public FlujoMensajeResponseDto actualizar(Principal principal, @PathVariable @Positive Long procesoId,
            @PathVariable @Positive Long id, @Valid @RequestBody FlujoMensajeRequestDto datos) {
        return servicio.actualizar(sesion.id(principal), procesoId, id, datos);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> retirar(Principal principal, @PathVariable @Positive Long procesoId,
            @PathVariable @Positive Long id, @RequestParam @PositiveOrZero Long version,
            @RequestParam(defaultValue = "false") boolean confirmar) {
        servicio.retirar(sesion.id(principal), procesoId, id, version, confirmar);
        return ResponseEntity.noContent().build();
    }
}
