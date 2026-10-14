package com.proyecto.inicio.dto.request;

import com.proyecto.inicio.entity.enums.PoliticaSinCorrespondencia;
import com.proyecto.inicio.entity.enums.SentidoMensaje;
import com.proyecto.inicio.entity.enums.TipoCorrelacion;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MensajeRequestDto {
    @NotNull
    @Positive
    private Long nodoId;

    @NotNull
    private SentidoMensaje sentido;

    @NotBlank
    @Size(max = 150)
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
