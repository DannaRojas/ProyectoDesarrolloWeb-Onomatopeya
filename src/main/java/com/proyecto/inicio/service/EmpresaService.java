package com.proyecto.inicio.service;

import com.proyecto.inicio.dto.request.EmpresaRequestDto;
import com.proyecto.inicio.dto.response.EmpresaResponseDto;
import com.proyecto.inicio.entity.Empresa;
import com.proyecto.inicio.entity.Usuario;
import com.proyecto.inicio.entity.enums.EstadoUsuario;
import com.proyecto.inicio.entity.enums.RolAcceso;
import com.proyecto.inicio.exception.AccesoColaboracionException;
import com.proyecto.inicio.repository.EmpresaRepository;
import com.proyecto.inicio.repository.UsuarioRepository;

import jakarta.persistence.EntityNotFoundException;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

@Service
public class EmpresaService {

    private final EmpresaRepository empresaRepository;
    private final UsuarioRepository usuarioRepository;

    public EmpresaService(
            EmpresaRepository empresaRepository, UsuarioRepository usuarioRepository) {
        this.empresaRepository = empresaRepository;
        this.usuarioRepository = usuarioRepository;
    }

    /**
     * Regla de negocio: Crear empresa y su usuario administrador inicial en la misma transacción.
     */
    @Transactional
    public EmpresaResponseDto crearEmpresaConAdminInicial(
            EmpresaRequestDto request,
            String nombreAdmin,
            String correoAdmin,
            String passwordHashAdmin) {

        validarDatosObligatorios(request);
        String nit = request.getNit().trim();

        if (empresaRepository.existsByNit(nit)) {
            throw new IllegalArgumentException("Ya existe una empresa registrada con ese NIT");
        }

        String correoNormalizado = correoAdmin.strip().toLowerCase(Locale.ROOT);
        if (usuarioRepository.existeCorreoNormalizado(correoNormalizado)) {
            throw new IllegalArgumentException("Ya existe un usuario registrado con ese correo");
        }

        // 1. Guardar la empresa usando el patrón Builder de Lombok
        Empresa empresa =
                Empresa.builder()
                        .nombre(request.getNombre().trim())
                        .nit(nit)
                        .correoContacto(request.getCorreoContacto().trim())
                        .activo(true)
                        .build();

        Empresa empresaGuardada = empresaRepository.save(empresa);

        // 2. Guardar el usuario administrador inicial en la misma transacción
        Usuario adminInicial =
                Usuario.builder()
                        .empresa(empresaGuardada)
                        .nombre(nombreAdmin.trim())
                        .correo(correoNormalizado)
                        .passwordHash(passwordHashAdmin)
                        .rolAcceso(RolAcceso.ADMINISTRADOR)
                        .estado(EstadoUsuario.ACTIVO)
                        .build();

        usuarioRepository.save(adminInicial);

        return mapearRespuesta(empresaGuardada);
    }

    @Transactional(readOnly = true)
    public List<EmpresaResponseDto> consultarTodas() {
        return empresaRepository.buscarPorEstado(true).stream().map(this::mapearRespuesta).toList();
    }

    @Transactional(readOnly = true)
    public EmpresaResponseDto consultarPorId(Long id) {
        return mapearRespuesta(buscarEmpresa(id));
    }

    @Transactional
    public EmpresaResponseDto actualizar(Long actorId, Long id, EmpresaRequestDto request) {
        validarDatosObligatorios(request);
        Usuario actor = exigirEmpresa(actorId, id);
        if (actor.getRolAcceso() != RolAcceso.ADMINISTRADOR) {
            throw new AccesoColaboracionException();
        }

        Empresa empresa = buscarEmpresa(id);
        String nit = request.getNit().trim();

        if (!empresa.getNit().equals(nit) && empresaRepository.existsByNit(nit)) {
            throw new IllegalArgumentException("Ya existe una empresa registrada con ese NIT");
        }

        empresa.setNombre(request.getNombre().trim());
        empresa.setNit(nit);
        empresa.setCorreoContacto(request.getCorreoContacto().trim());

        return mapearRespuesta(empresaRepository.save(empresa));
    }

    /**
     * Regla de negocio: No borrar físicamente empresas. Realizar borrado lógico (desactivación).
     */
    @Transactional
    public void desactivar(Long id) {
        Empresa empresa = buscarEmpresa(id);
        empresa.setActivo(false);
        empresaRepository.save(empresa);
    }

    @Transactional(readOnly = true)
    public List<EmpresaResponseDto> consultarParaUsuario(Long actorId) {
        Usuario actor = usuarioActivo(actorId);
        return List.of(mapearRespuesta(actor.getEmpresa()));
    }

    @Transactional(readOnly = true)
    public EmpresaResponseDto consultarParaUsuario(Long actorId, Long empresaId) {
        return mapearRespuesta(exigirEmpresa(actorId, empresaId).getEmpresa());
    }

    private Usuario exigirEmpresa(Long actorId, Long empresaId) {
        Usuario actor = usuarioActivo(actorId);
        if (!actor.getEmpresa().getId().equals(empresaId)) {
            throw new AccesoColaboracionException();
        }
        return actor;
    }

    private Usuario usuarioActivo(Long actorId) {
        if (actorId == null) {
            throw new AccesoColaboracionException();
        }
        return usuarioRepository.buscarConEmpresa(actorId)
                .filter(usuario -> usuario.getEstado() == EstadoUsuario.ACTIVO)
                .filter(usuario -> Boolean.TRUE.equals(usuario.getEmpresa().getActivo()))
                .orElseThrow(AccesoColaboracionException::new);
    }

    private Empresa buscarEmpresa(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("El id de la empresa es obligatorio");
        }

        return empresaRepository
                .findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Empresa no encontrada"));
    }

    private void validarDatosObligatorios(EmpresaRequestDto request) {
        if (request == null) {
            throw new IllegalArgumentException("Los datos de la empresa son obligatorios");
        }

        if (esVacio(request.getNombre())
                || esVacio(request.getNit())
                || esVacio(request.getCorreoContacto())) {
            throw new IllegalArgumentException("Nombre, NIT y correo de contacto son obligatorios");
        }

        if (!request.getCorreoContacto().trim().contains("@")) {
            throw new IllegalArgumentException("El correo de contacto no tiene un formato válido");
        }
        if (request.getNit().strip().length() > 30) {
            throw new IllegalArgumentException("El NIT no puede superar 30 caracteres");
        }
    }

    private boolean esVacio(String valor) {
        return valor == null || valor.isBlank();
    }

    private EmpresaResponseDto mapearRespuesta(Empresa empresa) {
        return new EmpresaResponseDto(
                empresa.getId(),
                empresa.getNombre(),
                empresa.getNit(),
                empresa.getCorreoContacto());
    }
}
