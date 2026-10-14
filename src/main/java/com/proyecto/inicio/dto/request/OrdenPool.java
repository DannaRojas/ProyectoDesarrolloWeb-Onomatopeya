package com.proyecto.inicio.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

public record OrdenPool(@NotNull @Positive Long id, @NotNull @PositiveOrZero Long version) {}
