package com.proyecto.inicio.dto.request;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class LaneRequestDto {

    private Long poolId;
    private Long rolProcesoId;
    private Integer orden;
    private Integer altura;
}