package com.proyecto.inicio.dto.response;

public record LaneVista(
        Long id,
        Long poolId,
        Long rolProcesoId,
        String nombre,
        Integer orden,
        Integer altura,
        Long version) {}
