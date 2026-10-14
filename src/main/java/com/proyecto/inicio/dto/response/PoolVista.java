package com.proyecto.inicio.dto.response;

import com.proyecto.inicio.entity.enums.TipoParticipante;

public record PoolVista(
        Long id,
        String nombre,
        TipoParticipante tipoParticipante,
        Boolean propietario,
        Boolean cajaNegra,
        Integer posicionX,
        Integer posicionY,
        Integer ancho,
        Integer alto,
        Integer orden,
        Long version) {}
