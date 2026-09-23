package com.proyecto.inicio.dto.response;

import com.proyecto.inicio.entity.enums.TipoParticipante;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PoolResponseDto {

    private Long id;
    private Long procesoId;
    private String nombre;
    private TipoParticipante tipoParticipante;
    private Boolean propietario;
    private Boolean cajaNegra;
    private Integer posicionX;
    private Integer posicionY;
    private Integer ancho;
    private Integer alto;
    private Integer orden;
    private Boolean activo;
}
