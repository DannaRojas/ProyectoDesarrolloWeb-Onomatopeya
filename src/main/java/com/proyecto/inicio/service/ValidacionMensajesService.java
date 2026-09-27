package com.proyecto.inicio.service;

import com.proyecto.inicio.entity.*;
import com.proyecto.inicio.entity.enums.*;
import com.proyecto.inicio.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service @RequiredArgsConstructor
public class ValidacionMensajesService {
    private final MensajeRepository mensajes;
    private final CampoMensajeRepository campos;
    private final FlujoMensajeRepository flujos;
    private final ContextoColaboracionService contexto;

    // Un borrador puede guardarse incompleto. Las advertencias describen lo que falta.
    @Transactional(readOnly = true)
    public List<String> advertencias(Long usuarioId, Long procesoId) {
        contexto.proceso(usuarioId, procesoId, false, false);
        List<Mensaje> declaraciones = mensajes.listarActivosPorProceso(procesoId);
        List<FlujoMensaje> conexiones = flujos.listarActivos(procesoId);
        Map<Long, Mensaje> porNodo = declaraciones.stream().collect(Collectors.toMap(m -> m.getNodo().getId(), m -> m));
        Set<String> avisos = new LinkedHashSet<>();
        for (Mensaje m : declaraciones) {
            if (m.getNodo().getEstado() == EstadoNodo.RETIRADO || !Boolean.TRUE.equals(m.getNodo().getPool().getActivo()))
                avisos.add("Mensaje " + m.getId() + ": su nodo o pool fue retirado.");
            if (m.getNodo() instanceof Evento evento && evento.getTipo() == TipoEvento.INTERMEDIO
                    && m.getSentido() == SentidoMensaje.RECEPCION && clave(m) == null)
                avisos.add("Mensaje " + m.getId() + ": la recepción intermedia no tiene correlación.");
            boolean conectado = conexiones.stream().anyMatch(f -> m.getSentido() == SentidoMensaje.ENVIO
                    ? Objects.equals(id(f.getNodoOrigen()), m.getNodo().getId())
                    : Objects.equals(id(f.getNodoDestino()), m.getNodo().getId()));
            if (!conectado && (m.getSentido() == SentidoMensaje.ENVIO || !Boolean.TRUE.equals(m.getOrigenExterno())))
                avisos.add("Mensaje " + m.getId() + ": falta conectar su " + (m.getSentido() == SentidoMensaje.ENVIO ? "receptor." : "emisor."));
            for (Mensaje otro : declaraciones) {
                // No se considera ambiguo el par esperado envío/recepción de una misma conexión.
                if (m.getId() < otro.getId() && m.getSentido() == otro.getSentido()
                        && m.getNombre().equals(otro.getNombre()) && clave(m) != null && Objects.equals(clave(m), clave(otro)))
                    avisos.add("Mensajes " + m.getId() + " y " + otro.getId() + ": mismo nombre y correlación; revisa la ambigüedad.");
            }
        }
        for (FlujoMensaje f : conexiones) {
            Mensaje envio = porNodo.get(id(f.getNodoOrigen()));
            Mensaje recepcion = porNodo.get(id(f.getNodoDestino()));
            if (f.getNodoOrigen() != null && envio == null || f.getNodoDestino() != null && recepcion == null)
                avisos.add("Flujo " + f.getId() + ": falta una declaración activa en sus extremos.");
            if (envio != null && recepcion != null) {
                if (!envio.getNombre().equals(recepcion.getNombre()))
                    avisos.add("Flujo " + f.getId() + ": el nombre de envío y recepción no coincide.");
                if (!contrato(envio).equals(contrato(recepcion)))
                    avisos.add("Flujo " + f.getId() + ": los nombres o tipos de los campos no coinciden.");
                if (clave(envio) == null || clave(recepcion) == null || !Objects.equals(clave(envio), clave(recepcion)))
                    avisos.add("Flujo " + f.getId() + ": revisa la correlación entre envío y recepción.");
            }
        }
        return new ArrayList<>(avisos);
    }

    private Map<String, TipoDatoMensaje> contrato(Mensaje m) {
        return campos.listarActivos(m.getId()).stream().collect(Collectors.toMap(CampoMensaje::getNombre, CampoMensaje::getTipoDato));
    }

    private String clave(Mensaje m) {
        if (m.getCorrelacionTipo() == TipoCorrelacion.NEGOCIO) return "NEGOCIO:" + m.getCorrelacionNegocio();
        if (m.getCorrelacionTipo() == TipoCorrelacion.CAMPO && m.getCorrelacionCampo() != null)
            return "CAMPO:" + m.getCorrelacionCampo().getNombre() + ":" + m.getCorrelacionCampo().getTipoDato();
        return null;
    }

    private Long id(Nodo nodo) { return nodo == null ? null : nodo.getId(); }
}
