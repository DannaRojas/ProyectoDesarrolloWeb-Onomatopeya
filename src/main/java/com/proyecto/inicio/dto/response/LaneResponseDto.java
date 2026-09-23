package com.proyecto.inicio.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class LaneResponseDto {

    private Long id;
    private Long poolId;
    private Long rolProcesoId;
    private String nombreRolProceso;
    private Integer orden;
    private Integer altura;
    private Boolean activo;
}