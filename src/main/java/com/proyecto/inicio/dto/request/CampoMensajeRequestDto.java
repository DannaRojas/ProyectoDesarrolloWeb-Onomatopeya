package com.proyecto.inicio.dto.request;

import com.proyecto.inicio.entity.enums.TipoDatoMensaje;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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
public class CampoMensajeRequestDto {
    @NotBlank
    @Size(max = 100)
    private String nombre;

    @NotNull
    private TipoDatoMensaje tipoDato;

    @NotNull
    @PositiveOrZero
    private Integer orden;

    @PositiveOrZero
    private Long version;
}
