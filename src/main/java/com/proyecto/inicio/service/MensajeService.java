package com.proyecto.inicio.service;

import com.proyecto.inicio.dto.request.MensajeRequestDto;
import com.proyecto.inicio.dto.response.MensajeResponseDto;
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
public class MensajeService {
    private final MensajeRepository mensajes;
    private final CampoMensajeRepository campos;
    private final FlujoMensajeRepository flujos;
    private final UsoMensajeActividadRepository usos;
    private final ContextoColaboracionService contexto;

    public MensajeResponseDto crear(Long usuarioId, Long procesoId, @NotNull @Valid MensajeRequestDto datos) {
        Proceso proceso = contexto.proceso(usuarioId, procesoId, true, false);
        Nodo nodo = contexto.nodo(datos.getNodoId(), procesoId);
        if (mensajes.buscarPorNodo(nodo.getId()).isPresent())
            throw new IllegalStateException("Este nodo ya tiene una declaración de mensaje, incluso si fue retirada.");
        Mensaje mensaje = new Mensaje();
        mensaje.setNodo(nodo);
        aplicar(mensaje, datos);
        mensajes.saveAndFlush(mensaje);
        contexto.auditar(usuarioId, proceso, "MENSAJE", mensaje.getId(), AccionHistorial.CREAR, "Se definió el mensaje " + mensaje.getNombre());
        return respuesta(mensaje);
    }

    public MensajeResponseDto actualizar(Long usuarioId, Long procesoId, Long id, @NotNull @Valid MensajeRequestDto datos) {
        Proceso proceso = contexto.proceso(usuarioId, procesoId, true, false);
        Mensaje mensaje = buscar(id, procesoId);
        contexto.version(datos.getVersion(), mensaje.getVersion());
        if (!Objects.equals(datos.getNodoId(), mensaje.getNodo().getId()))
            throw new IllegalArgumentException("Una declaración no puede trasladarse a otro nodo.");
        if (datos.getSentido() != mensaje.getSentido())
            throw new IllegalArgumentException("El sentido del mensaje no puede cambiar mientras conserva sus relaciones.");
        contexto.nodo(mensaje.getNodo().getId(), procesoId);
        aplicar(mensaje, datos);
        mensajes.saveAndFlush(mensaje);
        contexto.auditar(usuarioId, proceso, "MENSAJE", id, AccionHistorial.ACTUALIZAR, "Se actualizó el mensaje " + mensaje.getNombre());
        return respuesta(mensaje);
    }

    @Transactional(readOnly = true)
    public List<MensajeResponseDto> listar(Long usuarioId, Long procesoId) {
        contexto.proceso(usuarioId, procesoId, false, false);
        return mensajes.listarActivosPorProceso(procesoId).stream().map(this::respuesta).toList();
    }

    public void retirar(Long usuarioId, Long procesoId, Long id, Long version, boolean confirmado) {
        Proceso proceso = contexto.proceso(usuarioId, procesoId, true, true);
        contexto.confirmar(confirmado);
        Mensaje mensaje = buscar(id, procesoId);
        contexto.version(version, mensaje.getVersion());
        mensaje.setActivo(false);
        campos.listarActivos(id).forEach(c -> c.setActivo(false));
        usos.listarActivos(id).forEach(u -> u.setActivo(false));
        flujos.buscarConectados(mensaje.getNodo().getId()).forEach(f -> f.setActivo(false));
        contexto.auditar(usuarioId, proceso, "MENSAJE", id, AccionHistorial.DESACTIVAR,
                "Se retiró el mensaje junto con sus campos, usos y conexiones; se conservan los registros.");
    }

    private void aplicar(Mensaje mensaje, MensajeRequestDto datos) {
        Nodo nodo = mensaje.getNodo();
        if (nodo instanceof Actividad actividad) {
            if (actividad.getTipo() != TipoActividad.ENVIO || datos.getSentido() != SentidoMensaje.ENVIO)
                throw new IllegalArgumentException("Solo una actividad de envío puede declarar un mensaje saliente.");
        } else if (nodo instanceof Evento evento) {
            if (evento.getNaturaleza() != NaturalezaEvento.MENSAJE || evento.getOperacionMensaje() == null
                    || !evento.getOperacionMensaje().name().equals(datos.getSentido().name()))
                throw new IllegalArgumentException("El sentido debe coincidir con el evento de mensaje.");
            if ((evento.getTipo() == TipoEvento.INICIO && datos.getSentido() == SentidoMensaje.ENVIO)
                    || (evento.getTipo() == TipoEvento.FIN && datos.getSentido() == SentidoMensaje.RECEPCION))
                throw new IllegalArgumentException("La fase del evento no admite este sentido de mensaje.");
        } else throw new IllegalArgumentException("El nodo no admite mensajes.");
        if (datos.getSentido() == SentidoMensaje.ENVIO
                && (Boolean.TRUE.equals(datos.getOrigenExterno()) || datos.getPoliticaSinCorrespondencia() != null))
            throw new IllegalArgumentException("Origen externo y política sin correspondencia aplican a recepción.");
        if (datos.getCorrelacionTipo() == TipoCorrelacion.CAMPO) {
            if (datos.getCorrelacionCampoId() == null || mensaje.getId() == null || datos.getCorrelacionNegocio() != null)
                throw new IllegalArgumentException("Primero crea el campo y luego selecciónalo como correlación.");
            CampoMensaje campo = campos.buscarActivo(datos.getCorrelacionCampoId(), mensaje.getId())
                    .orElseThrow(() -> new IllegalArgumentException("La correlación debe usar un campo activo del mismo mensaje."));
            mensaje.setCorrelacionCampo(campo);
        } else {
            if (datos.getCorrelacionCampoId() != null) throw new IllegalArgumentException("No corresponde indicar un campo de correlación.");
            mensaje.setCorrelacionCampo(null);
        }
        if (datos.getCorrelacionTipo() == TipoCorrelacion.NEGOCIO) {
            if (datos.getCorrelacionNegocio() == null || datos.getCorrelacionNegocio().isBlank())
                throw new IllegalArgumentException("Indica el identificador de negocio.");
        } else if (datos.getCorrelacionNegocio() != null && !datos.getCorrelacionNegocio().isBlank())
            throw new IllegalArgumentException("El identificador corresponde a una correlación de negocio.");
        mensaje.setNombre(datos.getNombre().strip());
        mensaje.setSentido(datos.getSentido());
        mensaje.setOrigenExterno(datos.getOrigenExterno());
        mensaje.setCorrelacionTipo(datos.getCorrelacionTipo());
        mensaje.setCorrelacionNegocio(datos.getCorrelacionTipo() == TipoCorrelacion.NEGOCIO ? datos.getCorrelacionNegocio().strip() : null);
        mensaje.setPoliticaSinCorrespondencia(datos.getPoliticaSinCorrespondencia());
    }

    Mensaje buscar(Long id, Long procesoId) {
        return mensajes.buscarActivo(id, procesoId).orElseThrow(() -> new EntityNotFoundException("Mensaje no encontrado."));
    }

    private MensajeResponseDto respuesta(Mensaje m) {
        return new MensajeResponseDto(m.getId(), m.getNodo().getId(), m.getSentido(), m.getNombre(),
                m.getOrigenExterno(), m.getCorrelacionTipo(), m.getCorrelacionNegocio(),
                m.getCorrelacionCampo() == null ? null : m.getCorrelacionCampo().getId(),
                m.getPoliticaSinCorrespondencia(), m.getActivo(), m.getVersion());
    }
}
