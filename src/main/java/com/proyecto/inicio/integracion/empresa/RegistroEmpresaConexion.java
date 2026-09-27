package com.proyecto.inicio.integracion.empresa;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record RegistroEmpresaConexion(
        @NotNull @Valid DatosEmpresaConexion empresa,
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
}
