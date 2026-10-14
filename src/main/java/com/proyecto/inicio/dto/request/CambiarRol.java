package com.proyecto.inicio.dto.request;

import com.proyecto.inicio.entity.enums.RolAcceso;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record CambiarRol(@NotNull RolAcceso rolAcceso, @NotNull @PositiveOrZero Long version) {}
