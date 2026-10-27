package com.proyecto.inicio.service;

import com.proyecto.inicio.entity.enums.EstadoUsuario;
import com.proyecto.inicio.repository.UsuarioRepository;

import lombok.RequiredArgsConstructor;

import java.security.Principal;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
@RequiredArgsConstructor
public class UsuarioSesionService {
    private final UsuarioRepository usuarios;

    public Long id(Principal principal) {
        // La identidad debe venir del login, nunca de un id enviado en el cuerpo.
        if (principal == null) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED, "Inicia sesión para continuar.");
        }

        return usuarios.buscarPorCorreoConEmpresa(principal.getName())
                .filter(usuario -> usuario.getEstado() == EstadoUsuario.ACTIVO)
                .filter(usuario -> Boolean.TRUE.equals(usuario.getEmpresa().getActivo()))
                .map(usuario -> usuario.getId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED, "La sesión no corresponde a un usuario."));
    }
}
