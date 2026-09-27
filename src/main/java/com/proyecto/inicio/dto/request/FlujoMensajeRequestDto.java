package com.proyecto.inicio.dto.request;

import com.proyecto.inicio.entity.enums.*;
import jakarta.validation.constraints.*;
import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class FlujoMensajeRequestDto {
    @NotNull @Positive
    private Long poolOrigenId;

    @NotNull @Positive
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
