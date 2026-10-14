package com.proyecto.inicio.dto.response;

import com.proyecto.inicio.entity.enums.AccionEstructura;
import com.proyecto.inicio.entity.enums.RecursoEstructura;
import com.proyecto.inicio.entity.enums.RolAcceso;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PermisoEstructuraResponseDto {
    private Long id;
    private Long procesoId;
    private RolAcceso rolAcceso;
    private RecursoEstructura recurso;
    private AccionEstructura accion;
    private Boolean permitido;
    private Long version;
}
