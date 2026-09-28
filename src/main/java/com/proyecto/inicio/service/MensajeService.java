package com.proyecto.inicio.service;

import com.proyecto.inicio.dto.MensajeDto.*;
import com.proyecto.inicio.entity.*;
import com.proyecto.inicio.entity.enums.*;
import com.proyecto.inicio.repository.*;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.util.*;
import java.util.stream.Collectors;

@Service @Validated @RequiredArgsConstructor
@Transactional
public class MensajeService {
    private final MensajeRepository mensajes;
    private final CampoMensajeRepository campos;
    private final FlujoMensajeRepository flujos;
    private final UsoMensajeActividadRepository usos;
    private final ContextoColaboracionService contexto;
    private final ModelMapper modelMapper;

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
        Nodo nodo = org.hibernate.Hibernate.unproxy(mensaje.getNodo(), Nodo.class);
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
        modelMapper.map(datos, mensaje);
        mensaje.setNombre(datos.getNombre().strip());
        mensaje.setCorrelacionNegocio(datos.getCorrelacionTipo() == TipoCorrelacion.NEGOCIO ? datos.getCorrelacionNegocio().strip() : null);
    }

    Mensaje buscar(Long id, Long procesoId) {
        return mensajes.buscarActivo(id, procesoId).orElseThrow(() -> new EntityNotFoundException("Mensaje no encontrado."));
    }

    private MensajeResponseDto respuesta(Mensaje m) {
        return modelMapper.map(m, MensajeResponseDto.class);
    }

    public CampoMensajeResponseDto crearCampo(Long usuarioId, Long procesoId, Long mensajeId, @NotNull @Valid CampoMensajeRequestDto datos) {
        Proceso proceso = contexto.proceso(usuarioId, procesoId, true, false);
        Mensaje mensaje = buscar(mensajeId, procesoId);
        CampoMensaje campo = new CampoMensaje();
        campo.setMensaje(mensaje);
        aplicar(campo, datos);
        campos.saveAndFlush(campo);
        contexto.auditar(usuarioId, proceso, "CAMPO_MENSAJE", campo.getId(), AccionHistorial.CREAR, "Se agregó el campo " + campo.getNombre());
        return respuesta(campo);
    }

    public CampoMensajeResponseDto actualizarCampo(Long usuarioId, Long procesoId, Long mensajeId, Long id, @NotNull @Valid CampoMensajeRequestDto datos) {
        Proceso proceso = contexto.proceso(usuarioId, procesoId, true, false);
        Mensaje mensaje = buscar(mensajeId, procesoId);
        CampoMensaje campo = buscarCampo(id, mensajeId);
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
    public List<CampoMensajeResponseDto> listarCampos(Long usuarioId, Long procesoId, Long mensajeId) {
        contexto.proceso(usuarioId, procesoId, false, false);
        buscar(mensajeId, procesoId);
        return campos.listarActivos(mensajeId).stream().map(this::respuesta).toList();
    }

    public void retirarCampo(Long usuarioId, Long procesoId, Long mensajeId, Long id, Long version, boolean confirmado) {
        Proceso proceso = contexto.proceso(usuarioId, procesoId, true, true);
        contexto.confirmar(confirmado);
        Mensaje mensaje = buscar(mensajeId, procesoId);
        CampoMensaje campo = buscarCampo(id, mensajeId);
        contexto.version(version, campo.getVersion());
        if (mensaje.getCorrelacionCampo() != null && Objects.equals(mensaje.getCorrelacionCampo().getId(), id))
            throw new IllegalStateException("El campo todavía se usa como clave de correlación.");
        campo.setActivo(false);
        contexto.auditar(usuarioId, proceso, "CAMPO_MENSAJE", id, AccionHistorial.DESACTIVAR, "Se retiró el campo " + campo.getNombre());
    }

    private void aplicar(CampoMensaje campo, CampoMensajeRequestDto datos) {
        if (campos.nombreOcupado(campo.getMensaje().getId(), datos.getNombre().strip(), campo.getId()))
            throw new IllegalStateException("El nombre ya está reservado dentro del mensaje.");
        modelMapper.map(datos, campo);
        campo.setNombre(datos.getNombre().strip());
    }

    private CampoMensaje buscarCampo(Long id, Long mensajeId) {
        return campos.buscarActivo(id, mensajeId).orElseThrow(() -> new EntityNotFoundException("Campo no encontrado."));
    }

    private CampoMensajeResponseDto respuesta(CampoMensaje c) {
        return modelMapper.map(c, CampoMensajeResponseDto.class);
    }

    public UsoMensajeActividadResponseDto crearUso(Long usuarioId, Long procesoId, Long mensajeId, @NotNull @Valid UsoMensajeActividadRequestDto datos) {
        Proceso proceso = contexto.proceso(usuarioId, procesoId, true, false);
        Mensaje mensaje = buscar(mensajeId, procesoId);
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
    public List<UsoMensajeActividadResponseDto> listarUsos(Long usuarioId, Long procesoId, Long mensajeId) {
        contexto.proceso(usuarioId, procesoId, false, false);
        buscar(mensajeId, procesoId);
        return usos.listarActivos(mensajeId).stream().map(this::respuesta).toList();
    }

    public void retirarUso(Long usuarioId, Long procesoId, Long mensajeId, Long id, Long version, boolean confirmado) {
        Proceso proceso = contexto.proceso(usuarioId, procesoId, true, true);
        contexto.confirmar(confirmado);
        buscar(mensajeId, procesoId);
        UsoMensajeActividad uso = usos.buscarActivo(id, mensajeId).orElseThrow(() -> new EntityNotFoundException("Uso no encontrado."));
        contexto.version(version, uso.getVersion());
        uso.setActivo(false);
        contexto.auditar(usuarioId, proceso, "USO_MENSAJE_ACTIVIDAD", id, AccionHistorial.DESACTIVAR, "Se retiró la asociación entre actividad y mensaje.");
    }

    private UsoMensajeActividadResponseDto respuesta(UsoMensajeActividad u) {
        return modelMapper.map(u, UsoMensajeActividadResponseDto.class);
    }

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
            if (org.hibernate.Hibernate.unproxy(m.getNodo()) instanceof Evento evento && evento.getTipo() == TipoEvento.INTERMEDIO
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
