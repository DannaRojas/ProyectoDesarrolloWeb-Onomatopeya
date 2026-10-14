package com.proyecto.inicio.dto.request;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record Desactivar(
        @NotNull
        @PositiveOrZero Long version, @NotNull @AssertTrue Boolean confirmar) {}
