package com.proyecto.inicio.dto.response;

import com.proyecto.inicio.entity.enums.*;
import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class UsoMensajeActividadResponseDto {
    private Long id;
    private Long mensajeId;
    private Long actividadId;
    private Boolean activo;
    private Long version;
}
