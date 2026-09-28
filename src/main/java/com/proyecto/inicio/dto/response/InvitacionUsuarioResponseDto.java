package com.proyecto.inicio.dto.response;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import java.time.OffsetDateTime;

// El token se entrega solo al crear la invitación; no debe escribirse en logs.
@Getter
@RequiredArgsConstructor
public class InvitacionUsuarioResponseDto {
    private final UsuarioResponseDto usuario;
    private final String tokenInvitacion;
    private final OffsetDateTime expiraEn;
}
