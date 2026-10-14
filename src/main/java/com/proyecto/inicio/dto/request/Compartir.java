package com.proyecto.inicio.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

public record Compartir(
        @NotNull
        @Positive Long empresaId,
        @NotNull Boolean permitido,
        @NotNull
        @PositiveOrZero Long version) {}
