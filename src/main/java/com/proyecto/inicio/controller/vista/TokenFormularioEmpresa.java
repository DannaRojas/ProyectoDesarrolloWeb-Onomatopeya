package com.proyecto.inicio.controller.vista;

import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.UUID;

@Component
public class TokenFormularioEmpresa {
    private static final String ATRIBUTO = TokenFormularioEmpresa.class.getName();

    public String obtener(HttpSession sesion) {
        synchronized (sesion) {
            String token = (String) sesion.getAttribute(ATRIBUTO);
            if (token == null) {
                token = UUID.randomUUID().toString();
                sesion.setAttribute(ATRIBUTO, token);
            }
            return token;
        }
    }

    public void validar(HttpSession sesion, String recibido) {
        // Evita enviar el formulario desde otra página. No sustituye el login.
        String esperado = sesion == null ? null : (String) sesion.getAttribute(ATRIBUTO);
        if (esperado == null || recibido == null || !MessageDigest.isEqual(
                esperado.getBytes(StandardCharsets.UTF_8), recibido.getBytes(StandardCharsets.UTF_8))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Vuelve a abrir el formulario e intenta de nuevo.");
        }
    }
}
