package com.proyecto.inicio.dto.response;

import com.proyecto.inicio.entity.enums.*;
import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class PermisoEstructuraResponseDto {
    private Long id;
    private Long procesoId;
    private RolAcceso rolAcceso;
    private RecursoEstructura recurso;
    private AccionEstructura accion;
    private Boolean permitido;
    private Long version;
}
