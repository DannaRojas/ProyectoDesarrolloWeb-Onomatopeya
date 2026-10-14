package com.proyecto.inicio.dto.response;

public record ArcoVista(
        Long id,
        Long poolId,
        Long origenId,
        Long destinoId,
        String etiqueta,
        String condicionSalida) {}
