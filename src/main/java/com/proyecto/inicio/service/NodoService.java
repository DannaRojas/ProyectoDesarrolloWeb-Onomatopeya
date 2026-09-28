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
import com.proyecto.inicio.dto.DiagramaDto.*;
import com.proyecto.inicio.dto.ProcesoDto.Retirada;
import com.proyecto.inicio.entity.enums.*;
import com.proyecto.inicio.repository.*;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import java.util.*;

@Service
@Validated
@RequiredArgsConstructor
public class NodoService {
    private final ActividadRepository actividadRepository;
    private final NodoRepository nodoRepository;
    private final ArcoRepository arcoRepository;
    private final ContextoColaboracionService contexto;
    private final LaneRepository lanes;
    private final MensajeRepository mensajes;
    private final MensajeService mensajeService;
    private final FlujoMensajeRepository flujos;
    private final UsoMensajeActividadRepository usos;

    @Transactional
    public NodoVista crear(@NotNull @Positive Long actorId, @NotNull @Positive Long procesoId,
            @NotNull @Valid NodoDatos datos) {
        Proceso proceso = contexto.proceso(actorId, procesoId, true, false);
        Nodo nodo = switch (datos.clase()) {
            case ACTIVIDAD -> new Actividad();
            case GATEWAY -> new Gateway();
            case EVENTO -> new Evento();
        };
        nodo.setProceso(proceso);
        aplicar(nodo, datos, false);
        nodoRepository.saveAndFlush(nodo);
        contexto.auditar(actorId, proceso, "NODO", nodo.getId(), AccionHistorial.CREAR, "Creación de " + datos.clase());
        return vista(nodo);
    }

    @Transactional
    public NodoVista actualizar(@NotNull @Positive Long actorId, @NotNull @Positive Long procesoId,
            @NotNull @Positive Long nodoId, @NotNull @Valid NodoDatos datos) {
        Proceso proceso = contexto.proceso(actorId, procesoId, true, false);
        Nodo nodo = contexto.nodo(nodoId, procesoId);
        if (clase(nodo) != datos.clase()) throw new IllegalArgumentException("El tipo de nodo no puede cambiar.");
        if (!nodo.getPool().getId().equals(datos.poolId())) throw new IllegalArgumentException("El nodo no puede trasladarse a otro pool.");
        var mensaje = mensajes.buscarPorNodo(nodoId).filter(m -> Boolean.TRUE.equals(m.getActivo()));
        if (mensaje.isPresent()) {
            SentidoMensaje sentido = mensaje.get().getSentido();
            if (nodo instanceof Actividad && datos.tipoActividad() != TipoActividad.ENVIO
                    || nodo instanceof Evento && (datos.naturalezaEvento() != NaturalezaEvento.MENSAJE
                        || datos.operacionMensaje() == null || !datos.operacionMensaje().name().equals(sentido.name())))
                throw new IllegalStateException("Retira el mensaje antes de cambiar la función de este nodo.");
        }
        aplicar(nodo, datos, true);
        nodoRepository.saveAndFlush(nodo);
        contexto.auditar(actorId, proceso, "NODO", nodoId, AccionHistorial.ACTUALIZAR, "Edición de nodo.");
        return vista(nodo);
    }

    private void aplicar(Nodo nodo, NodoDatos datos, boolean edicion) {
        Pool pool = contexto.pool(datos.poolId(), nodo.getProceso().getId());
        if (Boolean.TRUE.equals(pool.getCajaNegra())) throw new IllegalArgumentException("Una caja negra no admite nodos.");
        nodo.setPool(pool); nodo.setNombre(datos.nombre().strip()); nodo.setPosicionX(datos.posicionX());
        nodo.setPosicionY(datos.posicionY()); nodo.setAncho(datos.ancho()); nodo.setAlto(datos.alto());
        if (nodo instanceof Actividad actividad) {
            if (datos.laneId() == null || datos.tipoActividad() == null)
                throw new IllegalArgumentException("La actividad requiere lane y tipo.");
            Lane lane = lanes.findByIdAndPoolIdAndActivoTrue(datos.laneId(), pool.getId())
                    .filter(l -> Boolean.TRUE.equals(l.getRolProceso().getActivo()))
                    .orElseThrow(() -> new EntityNotFoundException("Lane activa no encontrada en el pool."));
            actividad.setLane(lane); actividad.setTipo(datos.tipoActividad());
            guardarActividad(actividad);
        } else if (nodo instanceof Gateway gateway) {
            if (edicion) actualizarGateway(gateway, datos.tipoGateway(), datos.direccionGateway());
            else { gateway.setTipo(datos.tipoGateway()); gateway.setDireccion(datos.direccionGateway()); }
        } else if (nodo instanceof Evento evento) {
            evento.setTipo(datos.tipoEvento()); evento.setNaturaleza(datos.naturalezaEvento());
            evento.setOperacionMensaje(datos.operacionMensaje()); guardarEvento(evento);
        }
    }

    @Transactional
    public Gateway actualizarGateway(Gateway gateway, TipoGateway tipo, DireccionGateway direccion) {
        if (tipo == null || direccion == null) throw new IllegalArgumentException("Indica tipo y dirección del gateway.");
        List<Arco> salidas = arcoRepository.salientes(gateway.getId());
        if (tipo != TipoGateway.PARALELO && salidas.stream()
                .anyMatch(a -> a.getCondicionSalida() == null || a.getCondicionSalida().isBlank()))
            throw new IllegalArgumentException("Las salidas del gateway requieren condiciones.");
        if (tipo == TipoGateway.PARALELO) salidas.forEach(a -> a.setCondicionSalida(null));
        gateway.setTipo(tipo); gateway.setDireccion(direccion);
        return gateway;
    }

    @Transactional
    public List<String> retirar(@NotNull @Positive Long actorId, @NotNull @Positive Long procesoId,
            @NotNull @Positive Long nodoId, @NotNull @Valid Retirada datos) {
        Proceso proceso = contexto.proceso(actorId, procesoId, true, true);
        contexto.version(datos.version(), proceso.getVersion());
        contexto.confirmar(Boolean.TRUE.equals(datos.confirmar()));
        Nodo nodo = contexto.nodo(nodoId, procesoId);
        if (nodo instanceof Actividad && flujos.listarActivos(procesoId).stream()
                .anyMatch(f -> f.getActividadError() != null && f.getActividadError().getId().equals(nodoId)))
            throw new IllegalStateException("Reasigna el manejo de errores antes de retirar esta actividad.");
        mensajes.buscarPorNodo(nodoId).filter(m -> Boolean.TRUE.equals(m.getActivo()))
                .ifPresent(m -> mensajeService.retirar(actorId, procesoId, m.getId(), m.getVersion(), true));
        arcoRepository.conectados(nodoId).forEach(a -> a.setActivo(false));
        flujos.buscarConectados(nodoId).forEach(f -> f.setActivo(false));
        if (nodo instanceof Actividad) {
            usos.buscarPorActividad(nodoId).forEach(u -> u.setActivo(false));
        }
        nodo.setEstado(EstadoNodo.RETIRADO);
        nodoRepository.flush();
        contexto.auditar(actorId, proceso, "NODO", nodoId, AccionHistorial.DESACTIVAR, "Retirada lógica de nodo y conexiones.");
        return advertencias(actorId, procesoId);
    }

    @Transactional(readOnly = true)
    public List<NodoVista> listar(@NotNull @Positive Long actorId, @NotNull @Positive Long procesoId) {
        contexto.proceso(actorId, procesoId, false, false, true);
        return nodoRepository.listarVigentes(procesoId).stream().map(this::vista).toList();
    }

    @Transactional(readOnly = true)
    public List<String> advertencias(@NotNull @Positive Long actorId, @NotNull @Positive Long procesoId) {
        contexto.proceso(actorId, procesoId, false, false, true);
        List<Nodo> nodos = nodoRepository.listarVigentes(procesoId).stream()
                .map(n -> org.hibernate.Hibernate.unproxy(n, Nodo.class)).toList();
        List<Arco> arcos = arcoRepository.listarActivos(procesoId);
        List<String> avisos = new ArrayList<>();
        if (nodos.isEmpty()) avisos.add("El diagrama no contiene nodos.");
        for (Nodo nodo : nodos) {
            boolean inicio = nodo instanceof Evento e && e.getTipo() == TipoEvento.INICIO;
            boolean fin = nodo instanceof Evento e && e.getTipo() == TipoEvento.FIN;
            var entradas = arcos.stream().filter(a -> a.getDestino().getId().equals(nodo.getId())).toList();
            var salidas = arcos.stream().filter(a -> a.getOrigen().getId().equals(nodo.getId())).toList();
            if (!inicio && entradas.isEmpty()) avisos.add("Nodo " + nodo.getId() + ": no tiene camino de entrada.");
            if (!fin && salidas.isEmpty()) avisos.add("Nodo " + nodo.getId() + ": no tiene camino de salida.");
            if (inicio && !entradas.isEmpty()) avisos.add("El evento de inicio tiene arcos entrantes.");
            if (fin && !salidas.isEmpty()) avisos.add("El evento de fin tiene arcos salientes.");
            if (nodo instanceof Gateway g) {
                if (g.getTipo() == null || g.getDireccion() == null) avisos.add("Gateway incompleto: " + g.getId());
                if (g.getDireccion() == DireccionGateway.DIVERGENCIA && salidas.size() < 2)
                    avisos.add("El gateway de divergencia requiere dos salidas.");
                if (g.getTipo() != TipoGateway.PARALELO && salidas.stream()
                        .anyMatch(a -> a.getCondicionSalida() == null || a.getCondicionSalida().isBlank()))
                    avisos.add("El gateway requiere condiciones en sus salidas.");
                if (g.getTipo() == TipoGateway.EXCLUSIVO && salidas.stream().map(Arco::getCondicionSalida)
                        .filter(Objects::nonNull).distinct().count() < salidas.size())
                    avisos.add("Revisa condiciones repetidas en el gateway exclusivo " + g.getId());
            }
            if (nodo instanceof Actividad a && (!Boolean.TRUE.equals(a.getLane().getActivo())
                    || !Boolean.TRUE.equals(a.getLane().getRolProceso().getActivo())))
                avisos.add("La actividad tiene una lane o rol retirado.");
        }
        for (Long poolId : nodos.stream().map(n -> n.getPool().getId()).distinct().toList()) {
            Set<Long> inicios = new HashSet<>(); Set<Long> finales = new HashSet<>();
            for (Nodo n : nodos) if (n.getPool().getId().equals(poolId) && n instanceof Evento e) {
                if (e.getTipo() == TipoEvento.INICIO) inicios.add(n.getId());
                if (e.getTipo() == TipoEvento.FIN) finales.add(n.getId());
            }
            if (inicios.isEmpty() || finales.isEmpty()) avisos.add("Pool " + poolId + ": requiere inicio y fin.");
            Set<Long> alcanzados = alcanzables(inicios, arcos, false);
            Set<Long> haciaFin = alcanzables(finales, arcos, true);
            if (nodos.stream().filter(n -> n.getPool().getId().equals(poolId))
                    .anyMatch(n -> !alcanzados.contains(n.getId()) || !haciaFin.contains(n.getId())))
                avisos.add("Pool " + poolId + ": hay nodos sin recorrido entre inicio y fin.");
        }
        return avisos;
    }

    private Set<Long> alcanzables(Set<Long> semillas, List<Arco> arcos, boolean inverso) {
        Set<Long> vistos = new HashSet<>(semillas); ArrayDeque<Long> cola = new ArrayDeque<>(semillas);
        while (!cola.isEmpty()) {
            Long id = cola.remove();
            for (Arco arco : arcos) {
                Long origen = inverso ? arco.getDestino().getId() : arco.getOrigen().getId();
                Long destino = inverso ? arco.getOrigen().getId() : arco.getDestino().getId();
                if (origen.equals(id) && vistos.add(destino)) cola.add(destino);
            }
        }
        return vistos;
    }

    private ClaseNodo clase(Nodo n) {
        return n instanceof Actividad ? ClaseNodo.ACTIVIDAD : n instanceof Gateway ? ClaseNodo.GATEWAY : ClaseNodo.EVENTO;
    }

    private NodoVista vista(Nodo entidad) {
        Nodo n = org.hibernate.Hibernate.unproxy(entidad, Nodo.class);
        Actividad a = n instanceof Actividad act ? act : null;
        Gateway g = n instanceof Gateway gate ? gate : null;
        Evento e = n instanceof Evento event ? event : null;
        return new NodoVista(n.getId(), clase(n), n.getPool().getId(), n.getNombre(), n.getPosicionX(), n.getPosicionY(),
                n.getAncho(), n.getAlto(), n.getEstado(), a == null ? null : a.getLane().getId(),
                a == null ? null : a.getTipo(), g == null ? null : g.getTipo(), g == null ? null : g.getDireccion(),
                e == null ? null : e.getTipo(), e == null ? null : e.getNaturaleza(), e == null ? null : e.getOperacionMensaje());
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
        if (Boolean.TRUE.equals(nodo.getPool().getCajaNegra())) throw new IllegalArgumentException("Una caja negra no admite nodos.");
    }
}
