package com.proyecto.inicio.controller.mensajeria;

import com.proyecto.inicio.dto.request.CampoMensajeRequestDto;
import com.proyecto.inicio.dto.response.CampoMensajeResponseDto;
import com.proyecto.inicio.service.CampoMensajeService;
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
@RequestMapping("/procesos/{procesoId}/mensajes/{mensajeId}/campos")
@Profile("conexion-empresa")
@RequiredArgsConstructor
public class CampoMensajeController {
    private final CampoMensajeService servicio;
    private final UsuarioSesionColaboracion sesion;

    @PostMapping
    public ResponseEntity<CampoMensajeResponseDto> crear(Principal principal, @PathVariable @Positive Long procesoId, @PathVariable @Positive Long mensajeId,
            @Valid @RequestBody CampoMensajeRequestDto datos) {
        return ResponseEntity.status(201).body(servicio.crear(sesion.id(principal), procesoId, mensajeId, datos));
    }

    @GetMapping
    public List<CampoMensajeResponseDto> listar(Principal principal, @PathVariable @Positive Long procesoId, @PathVariable @Positive Long mensajeId) {
        return servicio.listar(sesion.id(principal), procesoId, mensajeId);
    }

    @PutMapping("/{id}")
    public CampoMensajeResponseDto actualizar(Principal principal, @PathVariable @Positive Long procesoId, @PathVariable @Positive Long mensajeId,
            @PathVariable @Positive Long id, @Valid @RequestBody CampoMensajeRequestDto datos) {
        return servicio.actualizar(sesion.id(principal), procesoId, mensajeId, id, datos);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> retirar(Principal principal, @PathVariable @Positive Long procesoId, @PathVariable @Positive Long mensajeId,
            @PathVariable @Positive Long id, @RequestParam @PositiveOrZero Long version,
            @RequestParam(defaultValue = "false") boolean confirmar) {
        servicio.retirar(sesion.id(principal), procesoId, mensajeId, id, version, confirmar);
        return ResponseEntity.noContent().build();
    }
}
