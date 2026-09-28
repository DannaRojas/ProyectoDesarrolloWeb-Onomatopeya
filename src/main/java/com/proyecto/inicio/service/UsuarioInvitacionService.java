package com.proyecto.inicio.service;

import com.proyecto.inicio.dto.request.InvitarUsuarioRequestDto;
import com.proyecto.inicio.dto.response.InvitacionUsuarioResponseDto;
import com.proyecto.inicio.dto.response.UsuarioResponseDto;
import com.proyecto.inicio.entity.Usuario;
import com.proyecto.inicio.entity.enums.EstadoUsuario;
import com.proyecto.inicio.entity.enums.RolAcceso;
import com.proyecto.inicio.exception.AccesoColaboracionException;
import com.proyecto.inicio.repository.UsuarioConsultaRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Locale;

@Service
@Validated
@Transactional
@RequiredArgsConstructor
public class UsuarioInvitacionService {
    private final UsuarioConsultaRepository usuarios;
    private final ModelMapper modelMapper;
    private final SecureRandom aleatorio = new SecureRandom();

    // administradorId debe venir del contexto autenticado, no del cuerpo de una petición.
    public InvitacionUsuarioResponseDto invitar(@NotNull @Positive Long administradorId,
            @NotNull @Valid InvitarUsuarioRequestDto datos) {
        Usuario administrador = usuarios.buscarConEmpresa(administradorId)
                .filter(u -> u.getEstado() == EstadoUsuario.ACTIVO)
                .filter(u -> u.getRolAcceso() == RolAcceso.ADMINISTRADOR)
                .filter(u -> Boolean.TRUE.equals(u.getEmpresa().getActivo()))
                .orElseThrow(AccesoColaboracionException::new);

        String correo = datos.getCorreo().strip().toLowerCase(Locale.ROOT);
        if (usuarios.existeCorreoNormalizado(correo)) {
            throw new IllegalArgumentException("No se puede crear una invitación con ese correo.");
        }

        byte[] bytes = new byte[32];
        aleatorio.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);

        Usuario invitado = modelMapper.map(datos, Usuario.class);
        invitado.setNombre(datos.getNombre().strip());
        invitado.setCorreo(correo);
        invitado.setEmpresa(administrador.getEmpresa());
        invitado.setInvitadoPor(administrador);
        invitado.setEstado(EstadoUsuario.INVITADO);
        invitado.setTokenInvitacionHash(hash(token));
        // Mantener la misma precisión que conserva la columna de PostgreSQL.
        invitado.setInvitacionExpiraEn(OffsetDateTime.now(ZoneOffset.UTC).plusHours(24).truncatedTo(ChronoUnit.MICROS));
        usuarios.saveAndFlush(invitado);

        // Aquí se prepara la invitación; todavía no se envía correo ni se activa la cuenta.
        return new InvitacionUsuarioResponseDto(modelMapper.map(invitado, UsuarioResponseDto.class),
                token, invitado.getInvitacionExpiraEn());
    }

    private String hash(String token) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException excepcion) {
            throw new IllegalStateException("No se pudo preparar la invitación.", excepcion);
        }
    }
}
