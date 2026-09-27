package com.proyecto.inicio.service;

import com.proyecto.inicio.dto.request.FlujoMensajeRequestDto;
import com.proyecto.inicio.dto.response.FlujoMensajeResponseDto;
import com.proyecto.inicio.entity.*;
import com.proyecto.inicio.entity.enums.*;
import com.proyecto.inicio.repository.*;
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
public class FlujoMensajeService {
    private final FlujoMensajeRepository flujos;
    private final MensajeRepository mensajes;
    private final ContextoColaboracionService contexto;

    public FlujoMensajeResponseDto crear(Long usuarioId, Long procesoId, @NotNull @Valid FlujoMensajeRequestDto datos) {
        Proceso proceso = contexto.proceso(usuarioId, procesoId, true, false);
        FlujoMensaje flujo = new FlujoMensaje();
        flujo.setProceso(proceso);
        aplicar(flujo, datos);
        flujos.saveAndFlush(flujo);
        contexto.auditar(usuarioId, proceso, "FLUJO_MENSAJE", flujo.getId(), AccionHistorial.CREAR, "Se conectaron dos pools mediante un mensaje.");
        return respuesta(flujo);
    }

    public FlujoMensajeResponseDto actualizar(Long usuarioId, Long procesoId, Long id, @NotNull @Valid FlujoMensajeRequestDto datos) {
        Proceso proceso = contexto.proceso(usuarioId, procesoId, true, false);
        FlujoMensaje flujo = buscar(id, procesoId);
        contexto.version(datos.getVersion(), flujo.getVersion());
        aplicar(flujo, datos);
        flujos.saveAndFlush(flujo);
        contexto.auditar(usuarioId, proceso, "FLUJO_MENSAJE", id, AccionHistorial.ACTUALIZAR, "Se actualizaron los extremos o la política del flujo de mensaje.");
        return respuesta(flujo);
    }

    @Transactional(readOnly = true)
    public List<FlujoMensajeResponseDto> listar(Long usuarioId, Long procesoId) {
        contexto.proceso(usuarioId, procesoId, false, false);
        return flujos.listarActivos(procesoId).stream().map(this::respuesta).toList();
    }

    public void retirar(Long usuarioId, Long procesoId, Long id, Long version, boolean confirmado) {
        Proceso proceso = contexto.proceso(usuarioId, procesoId, true, true);
        contexto.confirmar(confirmado);
        FlujoMensaje flujo = buscar(id, procesoId);
        contexto.version(version, flujo.getVersion());
        flujo.setActivo(false);
        contexto.auditar(usuarioId, proceso, "FLUJO_MENSAJE", id, AccionHistorial.DESACTIVAR, "Se retiró el flujo de mensaje sin borrar sus extremos.");
    }

    private void aplicar(FlujoMensaje flujo, FlujoMensajeRequestDto d) {
        Long procesoId = flujo.getProceso().getId();
        Pool origen = contexto.pool(d.getPoolOrigenId(), procesoId);
        Pool destino = contexto.pool(d.getPoolDestinoId(), procesoId);
        if (Objects.equals(origen.getId(), destino.getId()))
            throw new IllegalArgumentException("Los mensajes deben cruzar entre pools distintos.");
        if (d.getNodoOrigenId() == null && d.getNodoDestinoId() == null)
            throw new IllegalArgumentException("El flujo necesita al menos un nodo de envío o recepción.");
        Nodo envio = extremo(d.getNodoOrigenId(), origen, SentidoMensaje.ENVIO);
        Nodo recepcion = extremo(d.getNodoDestinoId(), destino, SentidoMensaje.RECEPCION);
        if (envio == null) {
            Mensaje esperado = mensajes.buscarPorNodo(recepcion.getId()).orElseThrow();
            if (!Boolean.TRUE.equals(esperado.getOrigenExterno()))
                throw new IllegalArgumentException("La recepción desde una caja negra debe indicar origen externo.");
        }
        if (recepcion == null && d.getTipoDestino() == null)
            throw new IllegalArgumentException("Indica si el destino externo es correo, servicio web o cola.");
        if (recepcion != null && (d.getTipoDestino() != null || d.getPoliticaFallo() != null || d.getActividadErrorId() != null))
            throw new IllegalArgumentException("La configuración externa solo aplica a un destino de caja negra.");
        Actividad error = null;
        if (d.getPoliticaFallo() == PoliticaFallo.DERIVAR) {
            if (d.getActividadErrorId() == null) throw new IllegalArgumentException("Selecciona la actividad que trata el error.");
            Nodo nodo = contexto.nodo(d.getActividadErrorId(), procesoId);
            if (!(nodo instanceof Actividad) || !Objects.equals(nodo.getPool().getId(), origen.getId()))
                throw new IllegalArgumentException("La actividad de error debe estar en el pool emisor.");
            error = (Actividad) nodo;
        } else if (d.getActividadErrorId() != null)
            throw new IllegalArgumentException("La actividad de error corresponde a la política DERIVAR.");
        for (FlujoMensaje existente : flujos.listarActivos(procesoId)) {
            if (!Objects.equals(existente.getId(), flujo.getId())
                    && Objects.equals(existente.getPoolOrigen().getId(), origen.getId())
                    && Objects.equals(existente.getPoolDestino().getId(), destino.getId())
                    && Objects.equals(id(existente.getNodoOrigen()), d.getNodoOrigenId())
                    && Objects.equals(id(existente.getNodoDestino()), d.getNodoDestinoId()))
                throw new IllegalStateException("Ya existe este flujo de mensaje.");
        }
        flujo.setPoolOrigen(origen); flujo.setPoolDestino(destino);
        flujo.setNodoOrigen(envio); flujo.setNodoDestino(recepcion);
        flujo.setTipoDestino(d.getTipoDestino()); flujo.setPoliticaFallo(d.getPoliticaFallo());
        flujo.setActividadError(error);
    }

    private Nodo extremo(Long id, Pool pool, SentidoMensaje sentido) {
        if (id == null) {
            if (!Boolean.TRUE.equals(pool.getCajaNegra()))
                throw new IllegalArgumentException("Solo se omite el nodo de un participante de caja negra.");
            return null;
        }
        Nodo nodo = contexto.nodo(id, pool.getProceso().getId());
        if (!Objects.equals(nodo.getPool().getId(), pool.getId()))
            throw new IllegalArgumentException("El nodo no pertenece al pool indicado.");
        Mensaje mensaje = mensajes.buscarPorNodo(id).filter(m -> Boolean.TRUE.equals(m.getActivo()))
                .orElseThrow(() -> new IllegalArgumentException("El nodo necesita una declaración de mensaje activa."));
        if (mensaje.getSentido() != sentido) throw new IllegalArgumentException("El sentido del mensaje no corresponde al extremo.");
        return nodo;
    }

    private FlujoMensaje buscar(Long id, Long procesoId) {
        return flujos.buscarActivo(id, procesoId).orElseThrow(() -> new EntityNotFoundException("Flujo no encontrado."));
    }

    private Long id(Nodo nodo) { return nodo == null ? null : nodo.getId(); }

    private FlujoMensajeResponseDto respuesta(FlujoMensaje f) {
        return new FlujoMensajeResponseDto(f.getId(), f.getProceso().getId(), f.getPoolOrigen().getId(),
                f.getPoolDestino().getId(), id(f.getNodoOrigen()), id(f.getNodoDestino()),
                f.getTipoDestino(), f.getPoliticaFallo(), id(f.getActividadError()), f.getActivo(), f.getVersion());
    }
}
