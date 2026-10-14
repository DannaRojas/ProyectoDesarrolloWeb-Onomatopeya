package com.proyecto.inicio.dto.response;

import java.util.List;

public record RolVista(
        Long id,
        String nombre,
        String descripcion,
        Boolean activo,
        Long version,
        List<Long> procesosEnUso) {}
