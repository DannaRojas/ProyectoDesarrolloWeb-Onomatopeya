package com.proyecto.inicio.dto.response;

import com.proyecto.inicio.entity.enums.AccionHistorial;

import java.time.OffsetDateTime;

public record Cambio(
        Long id,
        Long actorId,
        OffsetDateTime fecha,
        String tipoObjeto,
        Long objetoId,
        AccionHistorial accion,
        String descripcion) {}
