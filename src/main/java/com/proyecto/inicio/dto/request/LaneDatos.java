package com.proyecto.inicio.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

public record LaneDatos(
        @NotNull
        @Positive Long rolProcesoId,
        @NotNull
        @PositiveOrZero Integer orden,
        @NotNull
        @Positive Integer altura,
        @PositiveOrZero Long version) {}
