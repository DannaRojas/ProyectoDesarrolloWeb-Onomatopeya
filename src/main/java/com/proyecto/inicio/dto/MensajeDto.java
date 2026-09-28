package com.proyecto.inicio.dto;

import com.proyecto.inicio.entity.enums.*;
import jakarta.validation.constraints.*;
import lombok.*;

public final class MensajeDto {
    private MensajeDto() {
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor
    public static class MensajeRequestDto {
        @NotNull @Positive
        private Long nodoId;

        @NotNull
        private SentidoMensaje sentido;

        @NotBlank @Size(max = 150)
        private String nombre;

        @NotNull
        private Boolean origenExterno;

        private TipoCorrelacion correlacionTipo;

        @Size(max = 150)
        private String correlacionNegocio;

        @Positive
        private Long correlacionCampoId;

        private PoliticaSinCorrespondencia politicaSinCorrespondencia;

        @PositiveOrZero
        private Long version;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor
    public static class MensajeResponseDto {
        private Long id;
        private Long nodoId;
        private SentidoMensaje sentido;
        private String nombre;
        private Boolean origenExterno;
        private TipoCorrelacion correlacionTipo;
        private String correlacionNegocio;
        private Long correlacionCampoId;
        private PoliticaSinCorrespondencia politicaSinCorrespondencia;
        private Boolean activo;
        private Long version;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor
    public static class CampoMensajeRequestDto {
        @NotBlank @Size(max = 100)
        private String nombre;

        @NotNull
        private TipoDatoMensaje tipoDato;

        @NotNull @PositiveOrZero
        private Integer orden;

        @PositiveOrZero
        private Long version;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor
    public static class CampoMensajeResponseDto {
        private Long id;
        private Long mensajeId;
        private String nombre;
        private TipoDatoMensaje tipoDato;
        private Integer orden;
        private Boolean activo;
        private Long version;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor
    public static class UsoMensajeActividadRequestDto {
        @NotNull @Positive
        private Long actividadId;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor
    public static class UsoMensajeActividadResponseDto {
        private Long id;
        private Long mensajeId;
        private Long actividadId;
        private Boolean activo;
        private Long version;
    }
}
