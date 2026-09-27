package com.proyecto.inicio.service;

import com.proyecto.inicio.dto.request.UsoMensajeActividadRequestDto;
import com.proyecto.inicio.dto.response.UsoMensajeActividadResponseDto;
import com.proyecto.inicio.entity.*;
import com.proyecto.inicio.entity.enums.*;
import com.proyecto.inicio.repository.UsoMensajeActividadRepository;
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
public class UsoMensajeActividadService {
    private final UsoMensajeActividadRepository usos;
    private final MensajeService mensajes;
    private final ContextoColaboracionService contexto;

    public UsoMensajeActividadResponseDto crear(Long usuarioId, Long procesoId, Long mensajeId, @NotNull @Valid UsoMensajeActividadRequestDto datos) {
        Proceso proceso = contexto.proceso(usuarioId, procesoId, true, false);
        Mensaje mensaje = mensajes.buscar(mensajeId, procesoId);
        Nodo nodo = contexto.nodo(datos.getActividadId(), procesoId);
        if (mensaje.getSentido() != SentidoMensaje.RECEPCION || !(nodo instanceof Actividad)
                || !Objects.equals(nodo.getPool().getId(), mensaje.getNodo().getPool().getId()))
            throw new IllegalArgumentException("La actividad debe usar un mensaje recibido en su mismo pool.");
        UsoMensajeActividad uso = usos.buscarRelacion(mensajeId, nodo.getId()).orElseGet(UsoMensajeActividad::new);
        if (uso.getId() != null && Boolean.TRUE.equals(uso.getActivo()))
            throw new IllegalStateException("La actividad ya está asociada al mensaje.");
        uso.setMensaje(mensaje); uso.setActividad((Actividad) nodo); uso.setActivo(true);
        usos.saveAndFlush(uso);
        contexto.auditar(usuarioId, proceso, "USO_MENSAJE_ACTIVIDAD", uso.getId(), AccionHistorial.CREAR, "Se vinculó una actividad con los datos recibidos.");
        return respuesta(uso);
    }

    @Transactional(readOnly = true)
    public List<UsoMensajeActividadResponseDto> listar(Long usuarioId, Long procesoId, Long mensajeId) {
        contexto.proceso(usuarioId, procesoId, false, false);
        mensajes.buscar(mensajeId, procesoId);
        return usos.listarActivos(mensajeId).stream().map(this::respuesta).toList();
    }

    public void retirar(Long usuarioId, Long procesoId, Long mensajeId, Long id, Long version, boolean confirmado) {
        Proceso proceso = contexto.proceso(usuarioId, procesoId, true, true);
        contexto.confirmar(confirmado);
        mensajes.buscar(mensajeId, procesoId);
        UsoMensajeActividad uso = usos.buscarActivo(id, mensajeId).orElseThrow(() -> new EntityNotFoundException("Uso no encontrado."));
        contexto.version(version, uso.getVersion());
        uso.setActivo(false);
        contexto.auditar(usuarioId, proceso, "USO_MENSAJE_ACTIVIDAD", id, AccionHistorial.DESACTIVAR, "Se retiró la asociación entre actividad y mensaje.");
    }

    private UsoMensajeActividadResponseDto respuesta(UsoMensajeActividad u) {
        return new UsoMensajeActividadResponseDto(u.getId(), u.getMensaje().getId(), u.getActividad().getId(), u.getActivo(), u.getVersion());
    }
}
