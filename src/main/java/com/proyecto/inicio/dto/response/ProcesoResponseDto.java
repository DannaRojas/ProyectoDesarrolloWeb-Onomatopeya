package com.proyecto.inicio.dto.response;

import com.proyecto.inicio.entity.enums.EstadoPublicacion;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProcesoResponseDto {

    private Long id;
    private Long empresaId;
    private Long creadorId;
    private String nombre;
    private String descripcion;
    private String categoria;
    private EstadoPublicacion estadoPublicacion;
    private Boolean activo;
    private OffsetDateTime fechaCreacion;
}