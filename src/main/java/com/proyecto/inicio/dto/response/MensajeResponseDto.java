package com.proyecto.inicio.dto.response;

import com.proyecto.inicio.entity.enums.*;
import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class MensajeResponseDto {
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
