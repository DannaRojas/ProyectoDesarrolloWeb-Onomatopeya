package com.proyecto.inicio.dto.response;

import java.util.List;

public record MensajeDetalle(
        MensajeResponseDto mensaje,
        List<CampoMensajeResponseDto> campos,
        List<UsoMensajeActividadResponseDto> usos) {}
