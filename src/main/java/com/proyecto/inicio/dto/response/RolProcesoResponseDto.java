package com.proyecto.inicio.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RolProcesoResponseDto {

    private Long id;
    private Long empresaId;
    private String nombre;
    private String descripcion;
    private Boolean activo;
}