package com.proyecto.inicio.dto.request;

import com.proyecto.inicio.entity.enums.*;
import jakarta.validation.constraints.*;
import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class UsoMensajeActividadRequestDto {
    @NotNull @Positive
    private Long actividadId;
}
