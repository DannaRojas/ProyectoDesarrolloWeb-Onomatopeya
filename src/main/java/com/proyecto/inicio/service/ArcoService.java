package com.proyecto.inicio.service;

import com.proyecto.inicio.entity.*;
import com.proyecto.inicio.entity.enums.NaturalezaEvento;
import com.proyecto.inicio.entity.enums.OperacionEventoMensaje;
import com.proyecto.inicio.entity.enums.TipoEvento;
import com.proyecto.inicio.repository.ArcoRepository;
import com.proyecto.inicio.repository.NodoRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service @RequiredArgsConstructor
public class ArcoService {
    private final NodoRepository nodoRepository;
    private final ArcoRepository arcoRepository;

    @Transactional
    public Arco crear(Long origenId, Long destinoId, String etiqueta, String condicionSalida) {
        if (origenId.equals(destinoId)) throw new IllegalArgumentException("Un nodo no puede conectarse consigo mismo");
        Nodo origen = nodoRepository.findById(origenId).orElseThrow(() -> new EntityNotFoundException("Nodo origen no encontrado"));
        Nodo destino = nodoRepository.findById(destinoId).orElseThrow(() -> new EntityNotFoundException("Nodo destino no encontrado"));
        if (!origen.getPool().getId().equals(destino.getPool().getId())) throw new IllegalArgumentException("El arco debe permanecer dentro del mismo pool; para conectar pools use FlujoMensaje");
        if (!origen.getProceso().getId().equals(destino.getProceso().getId())) throw new IllegalArgumentException("El arco debe permanecer dentro del mismo proceso");
        if (destino instanceof Evento evento
                && evento.getTipo() == TipoEvento.INICIO
                && evento.getNaturaleza() == NaturalezaEvento.MENSAJE
                && evento.getOperacionMensaje() == OperacionEventoMensaje.RECEPCION) {
            throw new IllegalArgumentException("Un evento de inicio que recibe mensaje no puede tener arcos de entrada");
        }
        if (arcoRepository.existsByOrigenIdAndDestinoId(origenId, destinoId)) throw new IllegalArgumentException("La conexión ya existe");

        return arcoRepository.save(Arco.builder().proceso(origen.getProceso()).pool(origen.getPool())
                .origen(origen).destino(destino).etiqueta(etiqueta).condicionSalida(condicionSalida).activo(true).build());
    }

    @Transactional
    public void retirarActividad(Long actividadId) {
        Actividad actividad = (Actividad) nodoRepository.findById(actividadId)
                .filter(Actividad.class::isInstance).orElseThrow(() -> new EntityNotFoundException("Actividad no encontrada"));
        actividad.setEstado(com.proyecto.inicio.entity.enums.EstadoNodo.RETIRADO);
        nodoRepository.save(actividad);
        arcoRepository.deleteByOrigenIdOrDestinoId(actividadId, actividadId);
    }
}
