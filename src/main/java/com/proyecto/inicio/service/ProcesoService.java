package com.proyecto.inicio.service;

import com.proyecto.inicio.dto.request.ProcesoRequestDto;
import com.proyecto.inicio.dto.response.ProcesoResponseDto;
import com.proyecto.inicio.entity.Empresa;
import com.proyecto.inicio.entity.Pool;
import com.proyecto.inicio.entity.Proceso;
import com.proyecto.inicio.entity.Usuario;
import com.proyecto.inicio.entity.enums.EstadoPublicacion;
import com.proyecto.inicio.entity.enums.TipoParticipante;
import com.proyecto.inicio.repository.EmpresaRepository;
import com.proyecto.inicio.repository.PoolRepository;
import com.proyecto.inicio.repository.ProcesoRepository;
import com.proyecto.inicio.repository.UsuarioRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class ProcesoService {

    private final ProcesoRepository procesoRepository;
    private final PoolRepository poolRepository;
    private final EmpresaRepository empresaRepository;
    private final UsuarioRepository usuarioRepository;

    @Transactional
    public ProcesoResponseDto crear(ProcesoRequestDto request) {

        Empresa empresa = empresaRepository.findById(request.getEmpresaId())
                .orElseThrow(() -> new EntityNotFoundException("Empresa no encontrada"));

        Usuario creador = usuarioRepository.findById(request.getCreadorId())
                .orElseThrow(() -> new EntityNotFoundException("Usuario creador no encontrado"));

        if (!Objects.equals(creador.getEmpresa().getId(), empresa.getId())) {
            throw new IllegalArgumentException(
                    "El creador debe pertenecer a la empresa del proceso");
        }

        if (procesoRepository.existsByEmpresaIdAndNombre(
                empresa.getId(), request.getNombre())) {
            throw new IllegalArgumentException(
                    "Ya existe un proceso con ese nombre en la empresa");
        }

        Proceso proceso = Proceso.builder()
                .empresa(empresa)
                .creador(creador)
                .nombre(request.getNombre())
                .descripcion(request.getDescripcion())
                .categoria(request.getCategoria())
                .estadoPublicacion(EstadoPublicacion.BORRADOR)
                .activo(true)
                .build();

        Proceso procesoGuardado = procesoRepository.save(proceso);

        Pool poolPropietario = Pool.builder()
                .proceso(procesoGuardado)
                .nombre(empresa.getNombre())
                .tipoParticipante(TipoParticipante.EMPRESA)
                .propietario(true)
                .cajaNegra(false)
                .posicionX(0)
                .posicionY(0)
                .ancho(1000)
                .alto(600)
                .orden(1)
                .activo(true)
                .build();

        poolRepository.save(poolPropietario);

        return convertirAResponse(procesoGuardado);
    }

    @Transactional(readOnly = true)
    public List<ProcesoResponseDto> listarPorEmpresa(Long empresaId) {
        return procesoRepository.findByEmpresaIdAndActivoTrue(empresaId)
                .stream()
                .map(this::convertirAResponse)
                .toList();
    }

    @Transactional
    public void retirar(Long procesoId, Long empresaId) {
        Proceso proceso = procesoRepository
                .findByIdAndEmpresaIdAndActivoTrue(procesoId, empresaId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Proceso no encontrado o no pertenece a la empresa"));

        proceso.setActivo(false);
        procesoRepository.save(proceso);
    }

    private ProcesoResponseDto convertirAResponse(Proceso proceso) {
        return new ProcesoResponseDto(
                proceso.getId(),
                proceso.getEmpresa().getId(),
                proceso.getCreador().getId(),
                proceso.getNombre(),
                proceso.getDescripcion(),
                proceso.getCategoria(),
                proceso.getEstadoPublicacion(),
                proceso.getActivo(),
                proceso.getFechaCreacion()
        );
    }
}