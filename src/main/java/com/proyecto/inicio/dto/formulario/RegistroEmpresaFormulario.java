package com.proyecto.inicio.dto.formulario;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class RegistroEmpresaFormulario extends EmpresaFormulario {
    @NotBlank(message = "Escribe el nombre del administrador.")
    @Size(max = 255, message = "El nombre admite hasta 255 caracteres.")
    private String nombreAdmin;

    @NotBlank(message = "Escribe el correo del administrador.")
    @Email(message = "Escribe un correo válido.")
    @Size(max = 255, message = "El correo admite hasta 255 caracteres.")
    private String correoAdmin;

    @NotBlank(message = "Escribe una contraseña.")
    @Size(min = 8, max = 128, message = "La contraseña debe tener entre 8 y 128 caracteres.")
    private String contrasenaAdmin;
}
