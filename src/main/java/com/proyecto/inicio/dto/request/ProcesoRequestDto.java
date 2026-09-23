package com.proyecto.inicio.dto.request;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProcesoRequestDto {

    private Long empresaId;
    private Long creadorId;
    private String nombre;
    private String descripcion;
    private String categoria;
}