package com.proyecto.inicio.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record RolDatos(
        @NotBlank
        @Size(max = 150) String nombre,
        @Size(max = 10000) String descripcion,
        @PositiveOrZero Long version) {}
