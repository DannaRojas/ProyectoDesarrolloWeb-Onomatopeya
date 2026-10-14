package com.proyecto.inicio.controller;

import com.proyecto.inicio.dto.request.AceptarInvitacion;
import com.proyecto.inicio.dto.request.CambiarRol;
import com.proyecto.inicio.dto.request.Desactivar;
import com.proyecto.inicio.dto.request.InvitarUsuarioRequestDto;
import com.proyecto.inicio.dto.response.InvitacionUsuarioResponseDto;
import com.proyecto.inicio.dto.response.UsuarioResponseDto;
import com.proyecto.inicio.service.UsuarioInvitacionService;
import com.proyecto.inicio.service.UsuarioService;
import com.proyecto.inicio.service.UsuarioSesionService;

import io.swagger.v3.oas.annotations.Operation;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;

import lombok.RequiredArgsConstructor;

import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.List;

@RestController
@RequiredArgsConstructor
@Profile("conexion-empresa")
@RequestMapping("/usuarios")
public class UsuarioController {
    private final UsuarioInvitacionService invitaciones;
    private final UsuarioService usuarios;
    private final UsuarioSesionService sesion;

    @Operation(summary = "Listar usuarios de la empresa")
    @GetMapping
    public List<UsuarioResponseDto> listar(Principal principal) {
        return usuarios.listarParaUsuario(sesion.id(principal));
    }

    @Operation(summary = "Consultar un usuario de la empresa")
    @GetMapping("/{id}")
    public UsuarioResponseDto consultar(Principal principal, @Positive @PathVariable Long id) {
        return usuarios.consultarParaUsuario(sesion.id(principal), id);
    }

    @Operation(summary = "Invitar usuario a la empresa")
    @PostMapping("/invitaciones")
    @ResponseStatus(HttpStatus.CREATED)
    public InvitacionUsuarioResponseDto invitar(
            Principal principal, @Valid @RequestBody InvitarUsuarioRequestDto datos) {
        return invitaciones.invitar(sesion.id(principal), datos);
    }

    @Operation(summary = "Cambiar rol de acceso de un usuario")
    @PatchMapping("/{id}/rol")
    public UsuarioResponseDto cambiarRol(
            Principal principal,
            @Positive
            @PathVariable Long id,
            @Valid
            @RequestBody CambiarRol datos) {
        return usuarios.cambiarRol(sesion.id(principal), id, datos);
    }

    @Operation(summary = "Desactivar un usuario")
    @PostMapping("/{id}/desactivar")
    public UsuarioResponseDto desactivar(
            Principal principal,
            @Positive
            @PathVariable Long id,
            @Valid
            @RequestBody Desactivar datos) {
        return usuarios.desactivar(sesion.id(principal), id, datos);
    }

    @Operation(summary = "Aceptar una invitación")
    @PostMapping("/invitaciones/aceptar")
    public UsuarioResponseDto aceptarInvitacion(@Valid @RequestBody AceptarInvitacion datos) {
        return invitaciones.aceptar(datos);
    }
}
