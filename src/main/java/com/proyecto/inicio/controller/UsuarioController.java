package com.proyecto.inicio.controller;

import com.proyecto.inicio.dto.UsuarioDto.AceptarInvitacion;
import com.proyecto.inicio.dto.UsuarioDto.CambiarRol;
import com.proyecto.inicio.dto.UsuarioDto.Desactivar;
import com.proyecto.inicio.dto.request.InvitarUsuarioRequestDto;
import com.proyecto.inicio.dto.response.InvitacionUsuarioResponseDto;
import com.proyecto.inicio.dto.response.UsuarioResponseDto;
import com.proyecto.inicio.service.UsuarioInvitacionService;
import com.proyecto.inicio.service.UsuarioService;
import com.proyecto.inicio.service.UsuarioSesionService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import java.security.Principal;
import java.util.List;
import org.springframework.http.HttpStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@Profile("conexion-empresa")
@RequestMapping("/usuarios")
public class UsuarioController {
    private final UsuarioInvitacionService invitaciones;
    private final UsuarioService usuarios;
    private final UsuarioSesionService sesion;

    @GetMapping
    public List<UsuarioResponseDto> listar(Principal principal) {
        return usuarios.listarParaUsuario(sesion.id(principal));
    }

    @GetMapping("/{id}")
    public UsuarioResponseDto consultar(Principal principal, @Positive @PathVariable Long id) {
        return usuarios.consultarParaUsuario(sesion.id(principal), id);
    }

    @PostMapping("/invitaciones")
    @ResponseStatus(HttpStatus.CREATED)
    public InvitacionUsuarioResponseDto invitar(Principal principal,
            @Valid @RequestBody InvitarUsuarioRequestDto datos) {
        return invitaciones.invitar(sesion.id(principal), datos);
    }

    @PatchMapping("/{id}/rol")
    public UsuarioResponseDto cambiarRol(Principal principal, @Positive @PathVariable Long id,
            @Valid @RequestBody CambiarRol datos) {
        return usuarios.cambiarRol(sesion.id(principal), id, datos);
    }

    @PostMapping("/{id}/desactivar")
    public UsuarioResponseDto desactivar(Principal principal, @Positive @PathVariable Long id,
            @Valid @RequestBody Desactivar datos) {
        return usuarios.desactivar(sesion.id(principal), id, datos);
    }

    @PostMapping("/invitaciones/aceptar")
    public UsuarioResponseDto aceptarInvitacion(@Valid @RequestBody AceptarInvitacion datos) {
        return invitaciones.aceptar(datos);
    }
}
