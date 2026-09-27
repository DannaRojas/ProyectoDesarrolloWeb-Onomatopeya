package com.proyecto.inicio.dto.response;

import com.proyecto.inicio.entity.enums.*;
import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class CampoMensajeResponseDto {
    private Long id;
    private Long mensajeId;
    private String nombre;
    private TipoDatoMensaje tipoDato;
    private Integer orden;
    private Boolean activo;
    private Long version;
}
