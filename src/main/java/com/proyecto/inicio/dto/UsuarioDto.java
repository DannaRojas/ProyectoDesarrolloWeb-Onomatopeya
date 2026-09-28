package com.proyecto.inicio.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.AssertTrue;
import com.proyecto.inicio.entity.enums.RolAcceso;

public final class UsuarioDto {
    private UsuarioDto() {
    }

    public record CambiarRol(@NotNull RolAcceso rolAcceso, @NotNull @PositiveOrZero Long version) {
    }

    public record Desactivar(@NotNull @PositiveOrZero Long version, @NotNull @AssertTrue Boolean confirmar) {
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
