package com.proyecto.inicio.service;

import com.proyecto.inicio.entity.*;
import com.proyecto.inicio.entity.enums.*;
import com.proyecto.inicio.exception.AccesoColaboracionException;
import com.proyecto.inicio.repository.*;
import jakarta.persistence.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Objects;

@Service
@RequiredArgsConstructor
public class ContextoColaboracionService {
    private final UsuarioRepository usuarios;
    private final ProcesoRepository procesos;
    private final PoolRepository pools;
    private final NodoRepository nodos;
    private final HistorialCambioRepository historial;
    private final AccesoProcesoRepository accesos;
    private final EntityManager entityManager;

    public Usuario usuario(Long id) {
        if (id == null) throw new AccesoColaboracionException();
        Usuario usuario = usuarios.findById(id).orElseThrow(AccesoColaboracionException::new);
        if (usuario.getEstado() != EstadoUsuario.ACTIVO || !Boolean.TRUE.equals(usuario.getEmpresa().getActivo()))
            throw new AccesoColaboracionException();
        return usuario;
    }

    // Todas las operaciones del módulo pasan por el mismo control de empresa y rol.
    public Proceso proceso(Long usuarioId, Long procesoId, boolean escritura, boolean soloAdministrador) {
        return proceso(usuarioId, procesoId, escritura, soloAdministrador, false);
    }

    public Proceso proceso(Long usuarioId, Long procesoId, boolean escritura, boolean soloAdministrador, boolean incluirInactivo) {
        Usuario usuario = usuario(usuarioId);
        Proceso proceso = procesos.findById(procesoId)
                .filter(p -> (Boolean.TRUE.equals(p.getActivo()) || incluirInactivo && !escritura)
                        && Boolean.TRUE.equals(p.getEmpresa().getActivo()))
                .orElseThrow(() -> new EntityNotFoundException("Proceso no encontrado."));
        boolean propietario = Objects.equals(usuario.getEmpresa().getId(), proceso.getEmpresa().getId());
        boolean compartido = Boolean.TRUE.equals(proceso.getActivo()) && !escritura && !soloAdministrador && accesos
                .findByProcesoIdAndEmpresaInvitadaIdAndActivoTrue(procesoId, usuario.getEmpresa().getId()).isPresent();
        if (!propietario && !compartido) throw new AccesoColaboracionException();
        if (escritura && usuario.getRolAcceso() != RolAcceso.ADMINISTRADOR && usuario.getRolAcceso() != RolAcceso.EDITOR)
            throw new AccesoColaboracionException();
        if (soloAdministrador && usuario.getRolAcceso() != RolAcceso.ADMINISTRADOR)
            throw new AccesoColaboracionException();
        // Serializa las escrituras de este módulo dentro del mismo diagrama.
        if (escritura) entityManager.lock(proceso, LockModeType.PESSIMISTIC_WRITE);
        return proceso;
    }

    public Pool pool(Long id, Long procesoId) {
        return pools.findByIdAndProcesoIdAndActivoTrue(id, procesoId)
                .orElseThrow(() -> new EntityNotFoundException("Pool no encontrado en el proceso."));
    }

    public Nodo nodo(Long id, Long procesoId) {
        // Al cargar una relación lazy, necesitamos recuperar el subtipo real del nodo.
        Nodo nodo = org.hibernate.Hibernate.unproxy(
                nodos.findById(id).orElseThrow(() -> new EntityNotFoundException("Nodo no encontrado.")), Nodo.class);
        if (!Objects.equals(nodo.getProceso().getId(), procesoId)
                || !Objects.equals(nodo.getPool().getProceso().getId(), procesoId)
                || nodo.getEstado() == EstadoNodo.RETIRADO
                || !Boolean.TRUE.equals(nodo.getPool().getActivo())
                || Boolean.TRUE.equals(nodo.getPool().getCajaNegra()))
            throw new IllegalArgumentException("El nodo debe estar vigente en un pool abierto del proceso.");
        return nodo;
    }

    public void version(Long recibida, Long actual) {
        if (recibida == null || !Objects.equals(recibida, actual))
            throw new IllegalStateException("El registro cambió. Consulta su versión actual antes de modificarlo.");
    }

    public void confirmar(boolean confirmado) {
        if (!confirmado) throw new IllegalArgumentException("Confirma la retirada antes de continuar.");
    }

    public void auditar(Long usuarioId, Proceso proceso, String objeto, Long id, AccionHistorial accion, String descripcion) {
        historial.save(HistorialCambio.builder().empresaContexto(proceso.getEmpresa()).proceso(proceso)
                .actor(usuario(usuarioId)).tipoActor(TipoActor.USUARIO).tipoObjeto(objeto)
                .objetoId(id).accion(accion).descripcion(descripcion).build());
    }
}
