package com.proyecto.inicio.dto.request;

import com.proyecto.inicio.entity.enums.ClaseNodo;
import com.proyecto.inicio.entity.enums.DireccionGateway;
import com.proyecto.inicio.entity.enums.NaturalezaEvento;
import com.proyecto.inicio.entity.enums.OperacionEventoMensaje;
import com.proyecto.inicio.entity.enums.TipoActividad;
import com.proyecto.inicio.entity.enums.TipoEvento;
import com.proyecto.inicio.entity.enums.TipoGateway;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record NodoDatos(
        @NotNull ClaseNodo clase,
        @NotNull
        @Positive Long poolId,
        @NotBlank
        @Size(max = 150) String nombre,
        @NotNull
        @PositiveOrZero Integer posicionX,
        @NotNull
        @PositiveOrZero Integer posicionY,
        @NotNull
        @Positive Integer ancho,
        @NotNull
        @Positive Integer alto,
        @Positive Long laneId,
        TipoActividad tipoActividad,
        TipoGateway tipoGateway,
        DireccionGateway direccionGateway,
        TipoEvento tipoEvento,
        NaturalezaEvento naturalezaEvento,
        OperacionEventoMensaje operacionMensaje) {}
