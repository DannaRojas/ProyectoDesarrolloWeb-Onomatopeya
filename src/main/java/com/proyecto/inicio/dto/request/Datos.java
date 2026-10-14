package com.proyecto.inicio.dto.request;

import com.proyecto.inicio.entity.enums.EstadoPublicacion;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record Datos(
        @NotBlank
        @Size(max = 150) String nombre,
        @Size(max = 10000) String descripcion,
        @Size(max = 100) String categoria,
        @NotNull EstadoPublicacion estadoPublicacion,
        @PositiveOrZero Long version) {}
