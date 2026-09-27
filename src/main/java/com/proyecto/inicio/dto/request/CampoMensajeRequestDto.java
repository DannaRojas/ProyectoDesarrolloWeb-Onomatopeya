package com.proyecto.inicio.dto.request;

import com.proyecto.inicio.entity.enums.*;
import jakarta.validation.constraints.*;
import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class CampoMensajeRequestDto {
    @NotBlank @Size(max = 100)
    private String nombre;

    @NotNull
    private TipoDatoMensaje tipoDato;

    @NotNull @PositiveOrZero
    private Integer orden;

    @PositiveOrZero
    private Long version;
}
