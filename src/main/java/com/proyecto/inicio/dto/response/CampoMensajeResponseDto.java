package com.proyecto.inicio.dto.response;

import com.proyecto.inicio.entity.enums.TipoDatoMensaje;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CampoMensajeResponseDto {
    private Long id;
    private Long mensajeId;
    private String nombre;
    private TipoDatoMensaje tipoDato;
    private Integer orden;
    private Boolean activo;
    private Long version;
}
