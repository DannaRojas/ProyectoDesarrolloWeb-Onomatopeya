package com.proyecto.inicio.service;

import com.proyecto.inicio.dto.request.RolProcesoRequestDto;
import com.proyecto.inicio.dto.response.RolProcesoResponseDto;
import com.proyecto.inicio.entity.Empresa;
import com.proyecto.inicio.entity.RolProceso;
import com.proyecto.inicio.repository.EmpresaRepository;
import com.proyecto.inicio.repository.RolProcesoRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RolProcesoService {

    private final RolProcesoRepository rolProcesoRepository;
    private final EmpresaRepository empresaRepository;

    @Transactional
    public RolProcesoResponseDto crear(RolProcesoRequestDto request) {

        Empresa empresa = empresaRepository.findById(request.getEmpresaId())
                .orElseThrow(() -> new EntityNotFoundException("Empresa no encontrada"));

        if (rolProcesoRepository.existsByEmpresaIdAndNombre(
                empresa.getId(), request.getNombre())) {
            throw new IllegalArgumentException(
                    "Ya existe un rol de proceso con ese nombre en la empresa");
        }

        RolProceso rolProceso = RolProceso.builder()
                .empresa(empresa)
                .nombre(request.getNombre())
                .descripcion(request.getDescripcion())
                .activo(true)
                .build();

        RolProceso rolGuardado = rolProcesoRepository.save(rolProceso);

        return convertirAResponse(rolGuardado);
    }

    @Transactional(readOnly = true)
    public List<RolProcesoResponseDto> listarPorEmpresa(Long empresaId) {
        return rolProcesoRepository.findByEmpresaIdAndActivoTrue(empresaId)
                .stream()
                .map(this::convertirAResponse)
                .toList();
    }

    private RolProcesoResponseDto convertirAResponse(RolProceso rolProceso) {
        return new RolProcesoResponseDto(
                rolProceso.getId(),
                rolProceso.getEmpresa().getId(),
                rolProceso.getNombre(),
                rolProceso.getDescripcion(),
                rolProceso.getActivo()
        );
    }
}