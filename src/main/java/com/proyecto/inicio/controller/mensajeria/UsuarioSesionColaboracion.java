package com.proyecto.inicio.controller.mensajeria;

import com.proyecto.inicio.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import java.security.Principal;

@Component
@RequiredArgsConstructor
public class UsuarioSesionColaboracion {
    private final UsuarioRepository usuarios;

    public Long id(Principal principal) {
        // La identidad debe venir del login, nunca de un id enviado en el cuerpo.
        if (principal == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Inicia sesión para continuar.");
        return usuarios.findByCorreo(principal.getName()).map(u -> u.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "La sesión no corresponde a un usuario."));
    }
}
