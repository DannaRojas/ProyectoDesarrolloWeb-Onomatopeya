package com.proyecto.inicio.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UsoMensajeActividadResponseDto {
    private Long id;
    private Long mensajeId;
    private Long actividadId;
    private Boolean activo;
    private Long version;
}
