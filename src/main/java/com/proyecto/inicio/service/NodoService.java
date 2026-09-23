package com.proyecto.inicio.service;

import com.proyecto.inicio.entity.*;
import com.proyecto.inicio.entity.enums.EstadoNodo;
import com.proyecto.inicio.repository.ActividadRepository;
import com.proyecto.inicio.repository.NodoRepository;
import com.proyecto.inicio.repository.ArcoRepository;
import com.proyecto.inicio.entity.enums.TipoEvento;
import com.proyecto.inicio.entity.enums.NaturalezaEvento;
import com.proyecto.inicio.entity.enums.OperacionEventoMensaje;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NodoService {
    private final ActividadRepository actividadRepository;
    private final NodoRepository nodoRepository;
    private final ArcoRepository arcoRepository;

    public NodoService(ActividadRepository actividadRepository, NodoRepository nodoRepository,
                       ArcoRepository arcoRepository) {
        this.actividadRepository = actividadRepository;
        this.nodoRepository = nodoRepository;
        this.arcoRepository = arcoRepository;
    }

    @Transactional
    public Actividad guardarActividad(Actividad actividad) {
        validarUbicacion(actividad);
        if (actividad.getLane() == null) throw new IllegalArgumentException("La actividad debe pertenecer a una lane");
        if (!actividad.getLane().getPool().getId().equals(actividad.getPool().getId())) throw new IllegalArgumentException("La lane debe pertenecer al mismo pool que la actividad");
        if (actividad.getNombre() == null || actividad.getNombre().isBlank()) throw new IllegalArgumentException("El nombre de la actividad es obligatorio");
        boolean repetida = actividad.getId() == null
                ? actividadRepository.existsByProcesoIdAndNombreIgnoreCase(actividad.getProceso().getId(), actividad.getNombre())
                : actividadRepository.existsByProcesoIdAndNombreIgnoreCaseAndIdNot(actividad.getProceso().getId(), actividad.getNombre(), actividad.getId());
        if (repetida) throw new IllegalArgumentException("El nombre de actividad ya existe en este proceso");
        if (actividad.getEstado() == null) actividad.setEstado(EstadoNodo.BORRADOR);
        return actividadRepository.save(actividad);
    }

    @Transactional
    public Gateway guardarGatewayBorrador(Gateway gateway) {
        validarUbicacion(gateway);
        if (gateway.getEstado() == null) gateway.setEstado(EstadoNodo.BORRADOR);
        return nodoRepository.save(gateway);
    }

    @Transactional
    public Evento guardarEvento(Evento evento) {
        validarUbicacion(evento);
        if (evento.getTipo() == null || evento.getNaturaleza() == null)
            throw new IllegalArgumentException("El evento debe indicar tipo y naturaleza");
        if (evento.getNaturaleza() == NaturalezaEvento.MENSAJE && evento.getOperacionMensaje() == null)
            throw new IllegalArgumentException("Un evento de mensaje debe indicar envío o recepción");
        if (evento.getNaturaleza() == NaturalezaEvento.NORMAL && evento.getOperacionMensaje() != null)
            throw new IllegalArgumentException("Solo los eventos de mensaje indican envío o recepción");
        if (evento.getTipo() == TipoEvento.INICIO
                && evento.getNaturaleza() == NaturalezaEvento.MENSAJE
                && evento.getOperacionMensaje() == OperacionEventoMensaje.RECEPCION
                && evento.getId() != null && arcoRepository.existsByDestinoIdAndActivoTrue(evento.getId()))
            throw new IllegalArgumentException("Un evento de inicio que recibe mensaje no puede tener arcos de entrada");
        if (evento.getEstado() == null) evento.setEstado(EstadoNodo.BORRADOR);
        return nodoRepository.save(evento);
    }

    private void validarUbicacion(Nodo nodo) {
        if (nodo.getProceso() == null || nodo.getPool() == null) throw new IllegalArgumentException("El nodo requiere proceso y pool");
        if (!nodo.getPool().getProceso().getId().equals(nodo.getProceso().getId())) throw new IllegalArgumentException("El pool debe pertenecer al proceso indicado");
    }
}
