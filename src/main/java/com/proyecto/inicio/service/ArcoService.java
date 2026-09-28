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
import com.proyecto.inicio.dto.DiagramaDto.*;
import com.proyecto.inicio.dto.ProcesoDto.Retirada;
import com.proyecto.inicio.entity.enums.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.validation.annotation.Validated;
import java.util.List;

@Service @RequiredArgsConstructor
@Validated
public class ArcoService {
    private final NodoRepository nodoRepository;
    private final ArcoRepository arcoRepository;
    private final ContextoColaboracionService contexto;
    private final NodoService nodos;

    @Transactional
    public ArcoVista crear(@NotNull @Positive Long actorId, @NotNull @Positive Long procesoId,
            @NotNull @Valid ArcoDatos datos) {
        Proceso proceso = contexto.proceso(actorId, procesoId, true, false);
        Nodo origen = contexto.nodo(datos.origenId(), procesoId);
        Nodo destino = contexto.nodo(datos.destinoId(), procesoId);
        validar(origen, destino, datos.condicionSalida());
        Arco arco = arcoRepository.buscarPar(origen.getId(), destino.getId()).orElse(null);
        if (arco != null) throw new IllegalStateException("Esta conexión ya existe o fue retirada.");
        arco = arcoRepository.saveAndFlush(Arco.builder().proceso(proceso).pool(origen.getPool())
                .origen(origen).destino(destino).etiqueta(datos.etiqueta()).condicionSalida(datos.condicionSalida()).activo(true).build());
        contexto.auditar(actorId, proceso, "ARCO", arco.getId(), AccionHistorial.CREAR, "Creación de arco.");
        return vista(arco);
    }

    @Transactional
    public ArcoVista actualizar(@NotNull @Positive Long actorId, @NotNull @Positive Long procesoId,
            @NotNull @Positive Long id, @NotNull @Valid ArcoDatos datos) {
        Proceso proceso = contexto.proceso(actorId, procesoId, true, false);
        Arco arco = arcoRepository.buscarActivo(id, procesoId).orElseThrow(() -> new EntityNotFoundException("Arco no encontrado."));
        Nodo origen = contexto.nodo(datos.origenId(), procesoId); Nodo destino = contexto.nodo(datos.destinoId(), procesoId);
        validar(origen, destino, datos.condicionSalida());
        if (arcoRepository.buscarPar(origen.getId(), destino.getId()).filter(a -> !a.getId().equals(id)).isPresent())
            throw new IllegalArgumentException("Ya existe esa conexión.");
        arco.setOrigen(origen); arco.setDestino(destino); arco.setPool(origen.getPool());
        arco.setEtiqueta(datos.etiqueta()); arco.setCondicionSalida(datos.condicionSalida());
        arcoRepository.saveAndFlush(arco);
        contexto.auditar(actorId, proceso, "ARCO", id, AccionHistorial.ACTUALIZAR, "Edición de arco.");
        return vista(arco);
    }

    @Transactional
    public List<String> retirar(@NotNull @Positive Long actorId, @NotNull @Positive Long procesoId,
            @NotNull @Positive Long id, @NotNull @Valid Retirada datos) {
        Proceso proceso = contexto.proceso(actorId, procesoId, true, true);
        contexto.version(datos.version(), proceso.getVersion()); contexto.confirmar(Boolean.TRUE.equals(datos.confirmar()));
        Arco arco = arcoRepository.buscarActivo(id, procesoId).orElseThrow(() -> new EntityNotFoundException("Arco no encontrado."));
        arco.setActivo(false); arcoRepository.flush();
        contexto.auditar(actorId, proceso, "ARCO", id, AccionHistorial.DESACTIVAR, "Retirada lógica de arco.");
        return nodos.advertencias(actorId, procesoId);
    }

    @Transactional(readOnly = true)
    public List<ArcoVista> listar(@NotNull @Positive Long actorId, @NotNull @Positive Long procesoId) {
        contexto.proceso(actorId, procesoId, false, false, true);
        return arcoRepository.listarActivos(procesoId).stream().map(this::vista).toList();
    }

    private void validar(Nodo origen, Nodo destino, String condicion) {
        if (origen.getId().equals(destino.getId()) || !origen.getPool().getId().equals(destino.getPool().getId()))
            throw new IllegalArgumentException("El arco debe unir nodos distintos del mismo pool.");
        if (destino instanceof Evento e && e.getTipo() == TipoEvento.INICIO
                || origen instanceof Evento eventoOrigen && eventoOrigen.getTipo() == TipoEvento.FIN)
            throw new IllegalArgumentException("El inicio no recibe arcos y el fin no tiene salidas.");
        if (origen instanceof Gateway g && g.getTipo() != null) {
            if (g.getTipo() == TipoGateway.PARALELO && condicion != null && !condicion.isBlank())
                throw new IllegalArgumentException("Un gateway paralelo no usa condiciones.");
            if (g.getTipo() != TipoGateway.PARALELO && (condicion == null || condicion.isBlank()))
                throw new IllegalArgumentException("La salida de este gateway requiere una condición.");
        } else if (condicion != null && !condicion.isBlank()) {
            throw new IllegalArgumentException("La condición corresponde a una salida de gateway.");
        }
    }

    private ArcoVista vista(Arco a) {
        return new ArcoVista(a.getId(), a.getPool().getId(), a.getOrigen().getId(), a.getDestino().getId(),
                a.getEtiqueta(), a.getCondicionSalida());
    }

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
        arcoRepository.conectados(actividadId).forEach(a -> a.setActivo(false));
    }
}
