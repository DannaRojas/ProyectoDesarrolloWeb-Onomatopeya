package com.proyecto.inicio.dto.response;

import com.proyecto.inicio.entity.enums.ClaseNodo;
import com.proyecto.inicio.entity.enums.DireccionGateway;
import com.proyecto.inicio.entity.enums.EstadoNodo;
import com.proyecto.inicio.entity.enums.NaturalezaEvento;
import com.proyecto.inicio.entity.enums.OperacionEventoMensaje;
import com.proyecto.inicio.entity.enums.TipoActividad;
import com.proyecto.inicio.entity.enums.TipoEvento;
import com.proyecto.inicio.entity.enums.TipoGateway;

public record NodoVista(
        Long id,
        ClaseNodo clase,
        Long poolId,
        String nombre,
        Integer posicionX,
        Integer posicionY,
        Integer ancho,
        Integer alto,
        EstadoNodo estado,
        Long laneId,
        TipoActividad tipoActividad,
        TipoGateway tipoGateway,
        DireccionGateway direccionGateway,
        TipoEvento tipoEvento,
        NaturalezaEvento naturalezaEvento,
        OperacionEventoMensaje operacionMensaje) {}
