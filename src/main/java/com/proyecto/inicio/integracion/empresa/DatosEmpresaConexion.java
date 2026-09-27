package com.proyecto.inicio.integracion.empresa;

import com.proyecto.inicio.dto.request.EmpresaRequestDto;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record DatosEmpresaConexion(
        @NotBlank @Size(max = 150) String nombre,
        @NotBlank @Size(max = 255) String nit,
        @NotBlank @Email @Size(max = 255) String correoContacto) {

    EmpresaRequestDto alDtoExistente() {
        return new EmpresaRequestDto(nombre.strip(), nit.strip(), correoContacto.strip());
    }
}
