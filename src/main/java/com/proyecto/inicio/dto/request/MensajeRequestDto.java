package com.proyecto.inicio.dto.request;

import com.proyecto.inicio.entity.enums.*;
import jakarta.validation.constraints.*;
import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class MensajeRequestDto {
    @NotNull @Positive
    private Long nodoId;

    @NotNull
    private SentidoMensaje sentido;

    @NotBlank @Size(max = 150)
    private String nombre;

    @NotNull
    private Boolean origenExterno;

    private TipoCorrelacion correlacionTipo;

    @Size(max = 150)
    private String correlacionNegocio;

    @Positive
    private Long correlacionCampoId;

    private PoliticaSinCorrespondencia politicaSinCorrespondencia;

    @PositiveOrZero
    private Long version;
}
