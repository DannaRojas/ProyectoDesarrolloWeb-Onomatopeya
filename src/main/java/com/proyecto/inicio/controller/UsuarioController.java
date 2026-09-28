package com.proyecto.inicio.controller;

import com.proyecto.inicio.dto.UsuarioDto.AceptarInvitacion;
import com.proyecto.inicio.dto.response.UsuarioResponseDto;
import com.proyecto.inicio.service.UsuarioInvitacionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@Profile("conexion-empresa")
@RequestMapping("/usuarios")
public class UsuarioController {
    private final UsuarioInvitacionService invitaciones;

    @PostMapping("/invitaciones/aceptar")
    public UsuarioResponseDto aceptarInvitacion(@Valid @RequestBody AceptarInvitacion datos) {
        return invitaciones.aceptar(datos);
    }
}
