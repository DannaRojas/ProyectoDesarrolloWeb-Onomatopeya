package com.proyecto.inicio.dto.request;

import com.proyecto.inicio.entity.enums.TipoParticipante;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record PoolDatos(
        @NotBlank
        @Size(max = 150) String nombre,
        @NotNull TipoParticipante tipoParticipante,
        @NotNull Boolean cajaNegra,
        @NotNull
        @PositiveOrZero Integer posicionX,
        @NotNull
        @PositiveOrZero Integer posicionY,
        @NotNull
        @Positive Integer ancho,
        @NotNull
        @Positive Integer alto,
        @NotNull
        @PositiveOrZero Integer orden,
        @PositiveOrZero Long version) {}
