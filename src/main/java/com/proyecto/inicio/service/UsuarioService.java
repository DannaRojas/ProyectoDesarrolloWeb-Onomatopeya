package com.proyecto.inicio.service;

import com.proyecto.inicio.dto.request.CambiarRol;
import com.proyecto.inicio.dto.request.Desactivar;
import com.proyecto.inicio.dto.response.UsuarioResponseDto;
import com.proyecto.inicio.entity.HistorialCambio;
import com.proyecto.inicio.entity.Usuario;
import com.proyecto.inicio.entity.enums.AccionHistorial;
import com.proyecto.inicio.entity.enums.EstadoUsuario;
import com.proyecto.inicio.entity.enums.RolAcceso;
import com.proyecto.inicio.entity.enums.TipoActor;
import com.proyecto.inicio.exception.AccesoColaboracionException;
import com.proyecto.inicio.repository.EmpresaRepository;
import com.proyecto.inicio.repository.HistorialCambioRepository;
import com.proyecto.inicio.repository.UsuarioRepository;

import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import lombok.RequiredArgsConstructor;

import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.util.List;

@Service
@Validated
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class UsuarioService {
    private final UsuarioRepository usuarios;
    private final EmpresaRepository empresaRepositorio;
    private final EmpresaService empresas;
    private final ModelMapper modelMapper;
    private final ContextoColaboracionService contexto;
    private final HistorialCambioRepository historial;

    public List<UsuarioResponseDto> listarParaUsuario(@NotNull @Positive Long actorId) {
        return listarPorEmpresa(contexto.usuario(actorId).getEmpresa().getId());
    }

    public UsuarioResponseDto consultarParaUsuario(
            @NotNull
            @Positive Long actorId, @NotNull @Positive Long usuarioId) {
        return consultar(contexto.usuario(actorId).getEmpresa().getId(), usuarioId);
    }

    @Transactional
    public UsuarioResponseDto cambiarRol(
            @NotNull
            @Positive Long actorId,
            @NotNull
            @Positive Long usuarioId,
            @NotNull
            @Valid CambiarRol datos) {
        Usuario admin = administradorBloqueado(actorId);
        Usuario usuario = usuarioDeEmpresa(admin, usuarioId);
        contexto.version(datos.version(), usuario.getVersion());
        if (usuario.getEstado() == EstadoUsuario.INACTIVO) {
            throw new IllegalArgumentException(
                    "No se puede cambiar el rol de un usuario inactivo.");
        }
        if (usuario.getRolAcceso() != datos.rolAcceso()) {
            conservarAdministrador(usuario, datos.rolAcceso() == RolAcceso.ADMINISTRADOR);
            RolAcceso anterior = usuario.getRolAcceso();
            usuario.setRolAcceso(datos.rolAcceso());
            usuarios.saveAndFlush(usuario);
            auditar(
                    admin,
                    usuario,
                    AccionHistorial.ACTUALIZAR,
                    "Rol de acceso: " + anterior + " → " + datos.rolAcceso());
        }
        return modelMapper.map(usuario, UsuarioResponseDto.class);
    }

    @Transactional
    public UsuarioResponseDto desactivar(
            @NotNull
            @Positive Long actorId,
            @NotNull
            @Positive Long usuarioId,
            @NotNull
            @Valid Desactivar datos) {
        Usuario admin = administradorBloqueado(actorId);
        Usuario usuario = usuarioDeEmpresa(admin, usuarioId);
        contexto.version(datos.version(), usuario.getVersion());
        contexto.confirmar(Boolean.TRUE.equals(datos.confirmar()));
        if (usuario.getEstado() == EstadoUsuario.INACTIVO) {
            throw new IllegalStateException("El usuario ya está inactivo.");
        }
        conservarAdministrador(usuario, false);
        usuario.setEstado(EstadoUsuario.INACTIVO);
        usuario.setTokenInvitacionHash(null);
        usuario.setInvitacionExpiraEn(null);
        usuarios.saveAndFlush(usuario);
        auditar(admin, usuario, AccionHistorial.DESACTIVAR, "Desactivación de usuario.");
        return modelMapper.map(usuario, UsuarioResponseDto.class);
    }

    private Usuario administradorBloqueado(Long actorId) {
        Long empresaId = usuarios.empresaIdDeUsuario(actorId)
                .orElseThrow(AccesoColaboracionException::new);
        empresaRepositorio.bloquear(empresaId)
                .orElseThrow(AccesoColaboracionException::new);
        Usuario admin = contexto.usuario(actorId);
        if (admin.getRolAcceso() != RolAcceso.ADMINISTRADOR)
            throw new AccesoColaboracionException();
        return admin;
    }

    private void conservarAdministrador(Usuario usuario, boolean seguiraSiendoAdministrador) {
        if (usuario.getRolAcceso() == RolAcceso.ADMINISTRADOR
                && usuario.getEstado() == EstadoUsuario.ACTIVO
                && !seguiraSiendoAdministrador
                && usuarios.contarPorEmpresaEstadoYRol(
                        usuario.getEmpresa().getId(), EstadoUsuario.ACTIVO,
                        RolAcceso.ADMINISTRADOR) <= 1) {
            throw new IllegalStateException("La empresa debe conservar un administrador activo.");
        }
    }

    private Usuario usuarioDeEmpresa(Usuario admin, Long usuarioId) {
        return usuarios.buscarEnEmpresa(admin.getEmpresa().getId(), usuarioId)
                .orElseThrow(
                        () -> new EntityNotFoundException("Usuario no encontrado en la empresa."));
    }

    private void auditar(
            Usuario admin, Usuario usuario, AccionHistorial accion, String descripcion) {
        // Se conserva al usuario para que sus procesos y cambios anteriores mantengan la
        // referencia.
        historial.save(
                HistorialCambio.builder()
                        .empresaContexto(admin.getEmpresa())
                        .actor(admin)
                        .tipoActor(TipoActor.USUARIO)
                        .tipoObjeto("USUARIO")
                        .objetoId(usuario.getId())
                        .accion(accion)
                        .descripcion(descripcion)
                        .build());
    }

    public List<UsuarioResponseDto> listarPorEmpresa(@NotNull @Positive Long empresaId) {
        empresas.consultarPorId(empresaId);
        return usuarios.listarPorEmpresa(empresaId).stream()
                .map(usuario -> modelMapper.map(usuario, UsuarioResponseDto.class))
                .toList();
    }

    public UsuarioResponseDto consultar(
            @NotNull
            @Positive Long empresaId, @NotNull @Positive Long usuarioId) {
        empresas.consultarPorId(empresaId);
        Usuario usuario =
                usuarios.buscarEnEmpresa(empresaId, usuarioId)
                        .orElseThrow(
                                () ->
                                        new EntityNotFoundException(
                                                "Usuario no encontrado en la empresa."));
        return modelMapper.map(usuario, UsuarioResponseDto.class);
    }
}
