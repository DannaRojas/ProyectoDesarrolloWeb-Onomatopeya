package com.proyecto.inicio.dto;

import com.proyecto.inicio.entity.enums.*;
import jakarta.validation.constraints.*;
import lombok.*;

public final class PermisoEstructuraDto {
    private PermisoEstructuraDto() {
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor
    public static class PermisoEstructuraRequestDto {
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

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor
    public static class PermisoEstructuraResponseDto {
        private Long id;
        private Long procesoId;
        private RolAcceso rolAcceso;
        private RecursoEstructura recurso;
        private AccionEstructura accion;
        private Boolean permitido;
        private Long version;
    }
}
