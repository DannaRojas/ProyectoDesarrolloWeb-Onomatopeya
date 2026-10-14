package com.proyecto.inicio.dto.request;

import com.proyecto.inicio.entity.enums.PoliticaFallo;
import com.proyecto.inicio.entity.enums.TipoDestinoExterno;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class FlujoMensajeRequestDto {
    @NotNull
    @Positive
    private Long poolOrigenId;

    @NotNull
    @Positive
    private Long poolDestinoId;

    @Positive
    private Long nodoOrigenId;

    @Positive
    private Long nodoDestinoId;

    private TipoDestinoExterno tipoDestino;

    private PoliticaFallo politicaFallo;

    @Positive
    private Long actividadErrorId;

    @PositiveOrZero
    private Long version;
}
