package com.proyecto.inicio.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record ArcoDatos(
        @NotNull
        @Positive Long origenId,
        @NotNull
        @Positive Long destinoId,
        @Size(max = 150) String etiqueta,
        @Size(max = 10000) String condicionSalida) {}
