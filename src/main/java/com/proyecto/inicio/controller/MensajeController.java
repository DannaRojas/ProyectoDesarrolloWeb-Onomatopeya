package com.proyecto.inicio.controller;

import com.proyecto.inicio.dto.MensajeDto.*;
import com.proyecto.inicio.service.MensajeService;
import com.proyecto.inicio.service.UsuarioSesionService;
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
    private final UsuarioSesionService sesion;

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
        return servicio.advertencias(sesion.id(principal), procesoId);
    }

    @PostMapping("/{mensajeId}/campos")
    public ResponseEntity<CampoMensajeResponseDto> crearCampo(Principal principal, @PathVariable @Positive Long procesoId, @PathVariable @Positive Long mensajeId,
            @Valid @RequestBody CampoMensajeRequestDto datos) {
        return ResponseEntity.status(201).body(servicio.crearCampo(sesion.id(principal), procesoId, mensajeId, datos));
    }

    @GetMapping("/{mensajeId}/campos")
    public List<CampoMensajeResponseDto> listarCampos(Principal principal, @PathVariable @Positive Long procesoId, @PathVariable @Positive Long mensajeId) {
        return servicio.listarCampos(sesion.id(principal), procesoId, mensajeId);
    }

    @PutMapping("/{mensajeId}/campos/{id}")
    public CampoMensajeResponseDto actualizarCampo(Principal principal, @PathVariable @Positive Long procesoId, @PathVariable @Positive Long mensajeId,
            @PathVariable @Positive Long id, @Valid @RequestBody CampoMensajeRequestDto datos) {
        return servicio.actualizarCampo(sesion.id(principal), procesoId, mensajeId, id, datos);
    }

    @DeleteMapping("/{mensajeId}/campos/{id}")
    public ResponseEntity<Void> retirarCampo(Principal principal, @PathVariable @Positive Long procesoId, @PathVariable @Positive Long mensajeId,
            @PathVariable @Positive Long id, @RequestParam @PositiveOrZero Long version,
            @RequestParam(defaultValue = "false") boolean confirmar) {
        servicio.retirarCampo(sesion.id(principal), procesoId, mensajeId, id, version, confirmar);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{mensajeId}/usos")
    public ResponseEntity<UsoMensajeActividadResponseDto> crearUso(Principal principal, @PathVariable @Positive Long procesoId, @PathVariable @Positive Long mensajeId,
            @Valid @RequestBody UsoMensajeActividadRequestDto datos) {
        return ResponseEntity.status(201).body(servicio.crearUso(sesion.id(principal), procesoId, mensajeId, datos));
    }

    @GetMapping("/{mensajeId}/usos")
    public List<UsoMensajeActividadResponseDto> listarUsos(Principal principal, @PathVariable @Positive Long procesoId, @PathVariable @Positive Long mensajeId) {
        return servicio.listarUsos(sesion.id(principal), procesoId, mensajeId);
    }

    @DeleteMapping("/{mensajeId}/usos/{id}")
    public ResponseEntity<Void> retirarUso(Principal principal, @PathVariable @Positive Long procesoId, @PathVariable @Positive Long mensajeId,
            @PathVariable @Positive Long id, @RequestParam @PositiveOrZero Long version,
            @RequestParam(defaultValue = "false") boolean confirmar) {
        servicio.retirarUso(sesion.id(principal), procesoId, mensajeId, id, version, confirmar);
        return ResponseEntity.noContent().build();
    }
}
