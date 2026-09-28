package com.proyecto.inicio.dto;

import com.proyecto.inicio.entity.enums.*;
import jakarta.validation.constraints.*;
import lombok.*;

public final class FlujoMensajeDto {
    private FlujoMensajeDto() {
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor
    public static class FlujoMensajeRequestDto {
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

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor
    public static class FlujoMensajeResponseDto {
        private Long id;
        private Long procesoId;
        private Long poolOrigenId;
        private Long poolDestinoId;
        private Long nodoOrigenId;
        private Long nodoDestinoId;
        private TipoDestinoExterno tipoDestino;
        private PoliticaFallo politicaFallo;
        private Long actividadErrorId;
        private Boolean activo;
        private Long version;
    }
}
