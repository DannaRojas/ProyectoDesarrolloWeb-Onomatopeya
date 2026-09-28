package com.proyecto.inicio.dto.request;

import com.proyecto.inicio.entity.enums.RolAcceso;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter @NoArgsConstructor
public class InvitarUsuarioRequestDto {
    @NotBlank @Size(max = 255)
    private String nombre;

    @NotBlank @Email @Size(max = 255)
    private String correo;

    @NotNull
    private RolAcceso rolAcceso;
}
