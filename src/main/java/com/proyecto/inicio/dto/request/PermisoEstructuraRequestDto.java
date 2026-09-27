package com.proyecto.inicio.dto.request;

import com.proyecto.inicio.entity.enums.*;
import jakarta.validation.constraints.*;
import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class PermisoEstructuraRequestDto {
    @NotNull
    private RolAcceso rolAcceso;

    @NotNull
    private RecursoEstructura recurso;

    @NotNull
    private AccionEstructura accion;

    @NotNull
    private Boolean permitido;

    @PositiveOrZero
    private Long version;
}
