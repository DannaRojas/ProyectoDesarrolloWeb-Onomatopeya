package com.proyecto.inicio.dto.response;

import com.proyecto.inicio.entity.enums.EstadoPublicacion;

import java.time.OffsetDateTime;

public record Ficha(
        Long id,
        Long empresaId,
        Long creadorId,
        String nombre,
        String descripcion,
        String categoria,
        EstadoPublicacion estadoPublicacion,
        Boolean activo,
        OffsetDateTime fechaCreacion,
        Long version) {}
