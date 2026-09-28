package com.proyecto.inicio.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public final class UsuarioDto {
    private UsuarioDto() {
    }

    public record AceptarInvitacion(
            @NotBlank @Pattern(regexp = "[A-Za-z0-9_-]{43}") String token,
            @NotBlank @Size(min = 8, max = 128) String contrasena) {
        @Override
        public String toString() {
            return "AceptarInvitacion[token=oculto, contrasena=oculta]";
        }
    }
}
