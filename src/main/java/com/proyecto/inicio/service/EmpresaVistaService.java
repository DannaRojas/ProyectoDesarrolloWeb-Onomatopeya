package com.proyecto.inicio.service;

import com.proyecto.inicio.dto.formulario.EmpresaFormulario;
import com.proyecto.inicio.dto.formulario.RegistroEmpresaFormulario;
import com.proyecto.inicio.dto.request.EmpresaRequestDto;
import com.proyecto.inicio.dto.response.EmpresaResponseDto;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

@Service
@Validated
@Profile("conexion-empresa")
@RequiredArgsConstructor
public class EmpresaVistaService {
    private final EmpresaService empresas;
    private final ModelMapper modelMapper;
    private final PasswordEncoder codificador;

    public EmpresaFormulario prepararEdicion(Long id) {
        return modelMapper.map(empresas.consultarPorId(id), EmpresaFormulario.class);
    }

    public EmpresaResponseDto registrar(@NotNull @Valid RegistroEmpresaFormulario formulario) {
        EmpresaRequestDto datos = modelMapper.map(formulario, EmpresaRequestDto.class);
        // La transacción del servicio original guarda la empresa y su administrador.
        return empresas.crearEmpresaConAdminInicial(datos, formulario.getNombreAdmin().strip(),
                formulario.getCorreoAdmin().strip(), codificador.encode(formulario.getContrasenaAdmin()));
    }

    public EmpresaResponseDto actualizar(Long id, @NotNull @Valid EmpresaFormulario formulario) {
        return empresas.actualizar(id, modelMapper.map(formulario, EmpresaRequestDto.class));
    }
}
