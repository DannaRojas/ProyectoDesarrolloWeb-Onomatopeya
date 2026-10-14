package com.proyecto.inicio.dto.request;

import com.proyecto.inicio.entity.enums.AccionEstructura;
import com.proyecto.inicio.entity.enums.RecursoEstructura;
import com.proyecto.inicio.entity.enums.RolAcceso;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
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
