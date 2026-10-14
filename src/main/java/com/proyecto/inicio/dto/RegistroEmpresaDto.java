package com.proyecto.inicio.dto;

import com.proyecto.inicio.dto.request.Administrador;
import com.proyecto.inicio.dto.request.DatosEmpresa;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public record RegistroEmpresaDto(
        @NotNull
        @Valid DatosEmpresa empresa, @NotNull @Valid Administrador administrador) {}
