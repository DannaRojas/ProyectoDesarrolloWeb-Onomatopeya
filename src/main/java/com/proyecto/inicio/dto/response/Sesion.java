package com.proyecto.inicio.dto.response;

import java.time.Instant;

public record Sesion(String token, String tipo, Instant expiraEn) {}
