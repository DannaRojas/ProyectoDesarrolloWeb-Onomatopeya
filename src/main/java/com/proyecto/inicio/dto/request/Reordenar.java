package com.proyecto.inicio.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record Reordenar(
        @NotNull
        @Size(min = 1, max = 1000) List<@NotNull @Valid OrdenLane> lanes) {}
