package com.proyecto.inicio.dto;

import com.proyecto.inicio.dto.request.EmpresaRequestDto;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record RegistroEmpresaDto(
        @NotNull @Valid DatosEmpresa empresa,
        @NotNull @Valid Administrador administrador) {

    public record Administrador(
            @NotBlank @Size(max = 255) String nombre,
            @NotBlank @Email @Size(max = 255) String correo,
            @NotBlank @Size(min = 8, max = 128) String contrasena) {

        @Override
        public String toString() {
            return "Administrador[datos reservados]";
        }
    }

    public record DatosEmpresa(
            @NotBlank @Size(max = 150) String nombre,
            @NotBlank @Size(max = 255) String nit,
            @NotBlank @Email @Size(max = 255) String correoContacto) {

        public EmpresaRequestDto alDtoExistente() {
            return new EmpresaRequestDto(nombre.strip(), nit.strip(), correoContacto.strip());
        }
    }
}
