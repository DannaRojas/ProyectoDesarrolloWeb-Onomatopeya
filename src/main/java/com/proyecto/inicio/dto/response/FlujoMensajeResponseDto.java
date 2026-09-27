package com.proyecto.inicio.dto.response;

import com.proyecto.inicio.entity.enums.*;
import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class FlujoMensajeResponseDto {
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
