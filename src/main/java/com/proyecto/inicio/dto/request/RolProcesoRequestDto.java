package com.proyecto.inicio.dto.request;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RolProcesoRequestDto {

    private Long empresaId;
    private String nombre;
    private String descripcion;
}