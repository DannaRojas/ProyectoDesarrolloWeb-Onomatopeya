package com.proyecto.inicio.controller;

import com.proyecto.inicio.dto.request.IniciarSesion;
import com.proyecto.inicio.dto.response.Sesion;

import io.swagger.v3.oas.annotations.Operation;

import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Locale;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequiredArgsConstructor
@Profile("conexion-empresa")
@RequestMapping("/auth")
public class SesionController {
    private final AuthenticationManager autenticador;
    private final JwtEncoder firmador;

    @Operation(
            summary = "Iniciar sesión",
            description = "Verifica el correo y la contraseña de un usuario activo y devuelve un token Bearer válido durante dos horas.")
    @PostMapping("/login")
    public Sesion iniciar(@Valid @RequestBody IniciarSesion datos) {
        String correo = datos.correo().strip().toLowerCase(Locale.ROOT);
        try {
            autenticador.authenticate(
                    new UsernamePasswordAuthenticationToken(correo, datos.contrasena()));
        } catch (AuthenticationException excepcion) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Credenciales inválidas.");
        }
        Instant ahora = Instant.now();
        Instant vencimiento = ahora.plus(2, ChronoUnit.HOURS);
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("onomatopeya")
                .subject(correo)
                .issuedAt(ahora)
                .expiresAt(vencimiento)
                .build();
        JwsHeader encabezado = JwsHeader.with(MacAlgorithm.HS256).build();
        JwtEncoderParameters parametros = JwtEncoderParameters.from(encabezado, claims);
        String token = firmador.encode(parametros).getTokenValue();
        return new Sesion(token, "Bearer", vencimiento);
    }
}
