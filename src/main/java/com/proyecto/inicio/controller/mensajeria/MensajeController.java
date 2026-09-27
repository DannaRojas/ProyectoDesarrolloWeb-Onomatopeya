package com.proyecto.inicio.controller.mensajeria;

import com.proyecto.inicio.dto.request.MensajeRequestDto;
import com.proyecto.inicio.dto.response.MensajeResponseDto;
import com.proyecto.inicio.service.MensajeService;
import com.proyecto.inicio.service.ValidacionMensajesService;
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
@RequestMapping("/procesos/{procesoId}/mensajes")
@Profile("conexion-empresa")
@RequiredArgsConstructor
public class MensajeController {
    private final MensajeService servicio;
    private final UsuarioSesionColaboracion sesion;
    private final ValidacionMensajesService validacion;

    @PostMapping
    public ResponseEntity<MensajeResponseDto> crear(Principal principal, @PathVariable @Positive Long procesoId,
            @Valid @RequestBody MensajeRequestDto datos) {
        return ResponseEntity.status(201).body(servicio.crear(sesion.id(principal), procesoId, datos));
    }

    @GetMapping
    public List<MensajeResponseDto> listar(Principal principal, @PathVariable @Positive Long procesoId) {
        return servicio.listar(sesion.id(principal), procesoId);
    }

    @PutMapping("/{id}")
    public MensajeResponseDto actualizar(Principal principal, @PathVariable @Positive Long procesoId,
            @PathVariable @Positive Long id, @Valid @RequestBody MensajeRequestDto datos) {
        return servicio.actualizar(sesion.id(principal), procesoId, id, datos);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> retirar(Principal principal, @PathVariable @Positive Long procesoId,
            @PathVariable @Positive Long id, @RequestParam @PositiveOrZero Long version,
            @RequestParam(defaultValue = "false") boolean confirmar) {
        servicio.retirar(sesion.id(principal), procesoId, id, version, confirmar);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/advertencias")
    public List<String> advertencias(Principal principal, @PathVariable @Positive Long procesoId) {
        return validacion.advertencias(sesion.id(principal), procesoId);
    }
}
