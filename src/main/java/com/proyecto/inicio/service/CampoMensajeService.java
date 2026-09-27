package com.proyecto.inicio.service;

import com.proyecto.inicio.dto.request.CampoMensajeRequestDto;
import com.proyecto.inicio.dto.response.CampoMensajeResponseDto;
import com.proyecto.inicio.entity.*;
import com.proyecto.inicio.entity.enums.AccionHistorial;
import com.proyecto.inicio.repository.CampoMensajeRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.util.List;
import java.util.Objects;

@Service @Validated @RequiredArgsConstructor
@Transactional
public class CampoMensajeService {
    private final CampoMensajeRepository campos;
    private final MensajeService mensajes;
    private final ContextoColaboracionService contexto;

    public CampoMensajeResponseDto crear(Long usuarioId, Long procesoId, Long mensajeId, @NotNull @Valid CampoMensajeRequestDto datos) {
        Proceso proceso = contexto.proceso(usuarioId, procesoId, true, false);
        Mensaje mensaje = mensajes.buscar(mensajeId, procesoId);
        CampoMensaje campo = new CampoMensaje();
        campo.setMensaje(mensaje);
        aplicar(campo, datos);
        campos.saveAndFlush(campo);
        contexto.auditar(usuarioId, proceso, "CAMPO_MENSAJE", campo.getId(), AccionHistorial.CREAR, "Se agregó el campo " + campo.getNombre());
        return respuesta(campo);
    }

    public CampoMensajeResponseDto actualizar(Long usuarioId, Long procesoId, Long mensajeId, Long id, @NotNull @Valid CampoMensajeRequestDto datos) {
        Proceso proceso = contexto.proceso(usuarioId, procesoId, true, false);
        Mensaje mensaje = mensajes.buscar(mensajeId, procesoId);
        CampoMensaje campo = buscar(id, mensajeId);
        contexto.version(datos.getVersion(), campo.getVersion());
        if (mensaje.getCorrelacionCampo() != null && Objects.equals(mensaje.getCorrelacionCampo().getId(), id)
                && (!campo.getNombre().equals(datos.getNombre().strip()) || campo.getTipoDato() != datos.getTipoDato()))
            throw new IllegalStateException("Retira primero la selección de este campo como clave de correlación.");
        aplicar(campo, datos);
        campos.saveAndFlush(campo);
        contexto.auditar(usuarioId, proceso, "CAMPO_MENSAJE", id, AccionHistorial.ACTUALIZAR, "Se actualizó el campo " + campo.getNombre());
        return respuesta(campo);
    }

    @Transactional(readOnly = true)
    public List<CampoMensajeResponseDto> listar(Long usuarioId, Long procesoId, Long mensajeId) {
        contexto.proceso(usuarioId, procesoId, false, false);
        mensajes.buscar(mensajeId, procesoId);
        return campos.listarActivos(mensajeId).stream().map(this::respuesta).toList();
    }

    public void retirar(Long usuarioId, Long procesoId, Long mensajeId, Long id, Long version, boolean confirmado) {
        Proceso proceso = contexto.proceso(usuarioId, procesoId, true, true);
        contexto.confirmar(confirmado);
        Mensaje mensaje = mensajes.buscar(mensajeId, procesoId);
        CampoMensaje campo = buscar(id, mensajeId);
        contexto.version(version, campo.getVersion());
        if (mensaje.getCorrelacionCampo() != null && Objects.equals(mensaje.getCorrelacionCampo().getId(), id))
            throw new IllegalStateException("El campo todavía se usa como clave de correlación.");
        campo.setActivo(false);
        contexto.auditar(usuarioId, proceso, "CAMPO_MENSAJE", id, AccionHistorial.DESACTIVAR, "Se retiró el campo " + campo.getNombre());
    }

    private void aplicar(CampoMensaje campo, CampoMensajeRequestDto datos) {
        if (campos.nombreOcupado(campo.getMensaje().getId(), datos.getNombre().strip(), campo.getId()))
            throw new IllegalStateException("El nombre ya está reservado dentro del mensaje.");
        campo.setNombre(datos.getNombre().strip());
        campo.setTipoDato(datos.getTipoDato());
        campo.setOrden(datos.getOrden());
    }

    private CampoMensaje buscar(Long id, Long mensajeId) {
        return campos.buscarActivo(id, mensajeId).orElseThrow(() -> new EntityNotFoundException("Campo no encontrado."));
    }

    private CampoMensajeResponseDto respuesta(CampoMensaje c) {
        return new CampoMensajeResponseDto(c.getId(), c.getMensaje().getId(), c.getNombre(), c.getTipoDato(), c.getOrden(), c.getActivo(), c.getVersion());
    }
}
