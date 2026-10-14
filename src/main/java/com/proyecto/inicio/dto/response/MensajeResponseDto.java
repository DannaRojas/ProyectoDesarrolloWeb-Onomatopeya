package com.proyecto.inicio.dto.response;

import com.proyecto.inicio.entity.enums.PoliticaSinCorrespondencia;
import com.proyecto.inicio.entity.enums.SentidoMensaje;
import com.proyecto.inicio.entity.enums.TipoCorrelacion;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
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
