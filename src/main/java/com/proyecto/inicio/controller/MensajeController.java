package com.proyecto.inicio.controller;

import com.proyecto.inicio.dto.request.CampoMensajeRequestDto;
import com.proyecto.inicio.dto.request.MensajeRequestDto;
import com.proyecto.inicio.dto.request.UsoMensajeActividadRequestDto;
import com.proyecto.inicio.dto.response.CampoMensajeResponseDto;
import com.proyecto.inicio.dto.response.MensajeResponseDto;
import com.proyecto.inicio.dto.response.UsoMensajeActividadResponseDto;
import com.proyecto.inicio.service.MensajeService;
import com.proyecto.inicio.service.UsuarioSesionService;

import io.swagger.v3.oas.annotations.Operation;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

import lombok.RequiredArgsConstructor;

import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/procesos/{procesoId}/mensajes")
@Profile("conexion-empresa")
@RequiredArgsConstructor
public class MensajeController {
    private final MensajeService servicio;
    private final UsuarioSesionService sesion;

    @Operation(summary = "Definir un mensaje")
    @PostMapping
    public ResponseEntity<MensajeResponseDto> crear(
            Principal principal,
            @PathVariable
            @Positive Long procesoId,
            @Valid
            @RequestBody MensajeRequestDto datos) {
        return ResponseEntity.status(201)
                .body(servicio.crear(sesion.id(principal), procesoId, datos));
    }

    @Operation(summary = "Listar mensajes del proceso")
    @GetMapping
    public List<MensajeResponseDto> listar(
            Principal principal, @PathVariable @Positive Long procesoId) {
        return servicio.listar(sesion.id(principal), procesoId);
    }

    @Operation(summary = "Actualizar un mensaje")
    @PutMapping("/{id}")
    public MensajeResponseDto actualizar(
            Principal principal,
            @PathVariable
            @Positive Long procesoId,
            @PathVariable
            @Positive Long id,
            @Valid
            @RequestBody MensajeRequestDto datos) {
        return servicio.actualizar(sesion.id(principal), procesoId, id, datos);
    }

    @Operation(summary = "Retirar un mensaje")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> retirar(
            Principal principal,
            @PathVariable
            @Positive Long procesoId,
            @PathVariable
            @Positive Long id,
            @RequestParam
            @PositiveOrZero Long version,
            @RequestParam(defaultValue = "false") boolean confirmar) {
        servicio.retirar(sesion.id(principal), procesoId, id, version, confirmar);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Consultar advertencias de mensajería")
    @GetMapping("/advertencias")
    public List<String> advertencias(Principal principal, @PathVariable @Positive Long procesoId) {
        return servicio.advertencias(sesion.id(principal), procesoId);
    }

    @Operation(summary = "Agregar un campo al mensaje")
    @PostMapping("/{mensajeId}/campos")
    public ResponseEntity<CampoMensajeResponseDto> crearCampo(
            Principal principal,
            @PathVariable
            @Positive Long procesoId,
            @PathVariable
            @Positive Long mensajeId,
            @Valid
            @RequestBody CampoMensajeRequestDto datos) {
        return ResponseEntity.status(201)
                .body(servicio.crearCampo(sesion.id(principal), procesoId, mensajeId, datos));
    }

    @Operation(summary = "Listar campos del mensaje")
    @GetMapping("/{mensajeId}/campos")
    public List<CampoMensajeResponseDto> listarCampos(
            Principal principal,
            @PathVariable
            @Positive Long procesoId,
            @PathVariable
            @Positive Long mensajeId) {
        return servicio.listarCampos(sesion.id(principal), procesoId, mensajeId);
    }

    @Operation(summary = "Actualizar un campo del mensaje")
    @PutMapping("/{mensajeId}/campos/{id}")
    public CampoMensajeResponseDto actualizarCampo(
            Principal principal,
            @PathVariable
            @Positive Long procesoId,
            @PathVariable
            @Positive Long mensajeId,
            @PathVariable
            @Positive Long id,
            @Valid
            @RequestBody CampoMensajeRequestDto datos) {
        return servicio.actualizarCampo(sesion.id(principal), procesoId, mensajeId, id, datos);
    }

    @Operation(summary = "Retirar un campo del mensaje")
    @DeleteMapping("/{mensajeId}/campos/{id}")
    public ResponseEntity<Void> retirarCampo(
            Principal principal,
            @PathVariable
            @Positive Long procesoId,
            @PathVariable
            @Positive Long mensajeId,
            @PathVariable
            @Positive Long id,
            @RequestParam
            @PositiveOrZero Long version,
            @RequestParam(defaultValue = "false") boolean confirmar) {
        servicio.retirarCampo(sesion.id(principal), procesoId, mensajeId, id, version, confirmar);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Vincular una actividad a un mensaje recibido")
    @PostMapping("/{mensajeId}/usos")
    public ResponseEntity<UsoMensajeActividadResponseDto> crearUso(
            Principal principal,
            @PathVariable
            @Positive Long procesoId,
            @PathVariable
            @Positive Long mensajeId,
            @Valid
            @RequestBody UsoMensajeActividadRequestDto datos) {
        return ResponseEntity.status(201)
                .body(servicio.crearUso(sesion.id(principal), procesoId, mensajeId, datos));
    }

    @Operation(summary = "Listar actividades vinculadas al mensaje")
    @GetMapping("/{mensajeId}/usos")
    public List<UsoMensajeActividadResponseDto> listarUsos(
            Principal principal,
            @PathVariable
            @Positive Long procesoId,
            @PathVariable
            @Positive Long mensajeId) {
        return servicio.listarUsos(sesion.id(principal), procesoId, mensajeId);
    }

    @Operation(summary = "Retirar el uso de un mensaje")
    @DeleteMapping("/{mensajeId}/usos/{id}")
    public ResponseEntity<Void> retirarUso(
            Principal principal,
            @PathVariable
            @Positive Long procesoId,
            @PathVariable
            @Positive Long mensajeId,
            @PathVariable
            @Positive Long id,
            @RequestParam
            @PositiveOrZero Long version,
            @RequestParam(defaultValue = "false") boolean confirmar) {
        servicio.retirarUso(sesion.id(principal), procesoId, mensajeId, id, version, confirmar);
        return ResponseEntity.noContent().build();
    }
}
