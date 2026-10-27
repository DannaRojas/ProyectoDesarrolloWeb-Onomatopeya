package com.proyecto.inicio.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record IniciarSesion(@NotBlank @Email String correo, @NotBlank String contrasena) {}
