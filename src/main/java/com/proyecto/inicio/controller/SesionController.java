package com.proyecto.inicio.controller;

import com.proyecto.inicio.dto.request.IniciarSesion;
import com.proyecto.inicio.dto.response.Sesion;
import com.proyecto.inicio.service.SesionService;

import io.swagger.v3.oas.annotations.Operation;

import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;

import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Profile("conexion-empresa")
@RequestMapping("/auth")
public class SesionController {
    private final SesionService sesiones;

    @Operation(
            summary = "Iniciar sesión",
            description = "Verifica el correo y la contraseña de un usuario activo y devuelve un token Bearer válido durante dos horas.")
    @PostMapping("/login")
    public Sesion iniciar(@Valid @RequestBody IniciarSesion datos) {
        return sesiones.iniciar(datos);
    }
}
