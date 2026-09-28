package com.proyecto.inicio.service;

import com.proyecto.inicio.dto.response.UsuarioResponseDto;
import com.proyecto.inicio.repository.UsuarioConsultaRepository;
import jakarta.persistence.EntityNotFoundException;
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
public class UsuarioConsultaService {
    private final UsuarioConsultaRepository usuarios;
    private final EmpresaService empresas;
    private final ModelMapper modelMapper;

    // El futuro controlador debe verificar acceso a esta empresa con la sesión real.
    public List<UsuarioResponseDto> listarPorEmpresa(@NotNull @Positive Long empresaId) {
        empresas.consultarPorId(empresaId);
        return usuarios.listarPorEmpresa(empresaId).stream()
                .map(usuario -> modelMapper.map(usuario, UsuarioResponseDto.class))
                .toList();
    }

    public UsuarioResponseDto consultar(@NotNull @Positive Long empresaId, @NotNull @Positive Long usuarioId) {
        empresas.consultarPorId(empresaId);
        var usuario = usuarios.buscarEnEmpresa(empresaId, usuarioId)
                .orElseThrow(() -> new EntityNotFoundException("Usuario no encontrado en la empresa."));
        return modelMapper.map(usuario, UsuarioResponseDto.class);
    }
}
