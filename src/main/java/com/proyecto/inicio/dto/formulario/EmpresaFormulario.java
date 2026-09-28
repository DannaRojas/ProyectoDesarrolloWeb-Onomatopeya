package com.proyecto.inicio.dto.formulario;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class EmpresaFormulario {
    @NotBlank(message = "Escribe el nombre de la empresa.")
    @Size(max = 150, message = "El nombre admite hasta 150 caracteres.")
    private String nombre;

    @NotBlank(message = "Escribe el NIT.")
    @Size(max = 255, message = "El NIT admite hasta 255 caracteres.")
    private String nit;

    @NotBlank(message = "Escribe el correo de contacto.")
    @Email(message = "Escribe un correo válido.")
    @Size(max = 255, message = "El correo admite hasta 255 caracteres.")
    private String correoContacto;
}
