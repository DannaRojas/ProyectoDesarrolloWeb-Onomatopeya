package com.proyecto.inicio.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record DatosEmpresa(
        @NotBlank
        @Size(max = 150) String nombre,
        @NotBlank
        @Size(max = 30) String nit,
        @NotBlank
        @Email
        @Size(max = 255) String correoContacto) {

    public EmpresaRequestDto alDtoExistente() {
        return new EmpresaRequestDto(nombre.strip(), nit.strip(), correoContacto.strip());
    }
}
