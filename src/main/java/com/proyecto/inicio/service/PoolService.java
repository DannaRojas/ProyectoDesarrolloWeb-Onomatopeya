package com.proyecto.inicio.service;

import com.proyecto.inicio.dto.request.PoolRequestDto;
import com.proyecto.inicio.dto.response.PoolResponseDto;
import com.proyecto.inicio.entity.Pool;
import com.proyecto.inicio.entity.Proceso;
import com.proyecto.inicio.repository.PoolRepository;
import com.proyecto.inicio.repository.ProcesoRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.proyecto.inicio.repository.LaneRepository;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PoolService {

    private final PoolRepository poolRepository;
    private final ProcesoRepository procesoRepository;
    private final LaneRepository laneRepository;

    @Transactional
    public PoolResponseDto crearExterno(PoolRequestDto request) {

        Proceso proceso = procesoRepository.findById(request.getProcesoId())
                .orElseThrow(() -> new EntityNotFoundException("Proceso no encontrado"));

        Pool pool = Pool.builder()
                .proceso(proceso)
                .nombre(request.getNombre())
                .tipoParticipante(request.getTipoParticipante())
                .propietario(false)
                .cajaNegra(request.getCajaNegra())
                .posicionX(request.getPosicionX())
                .posicionY(request.getPosicionY())
                .ancho(request.getAncho())
                .alto(request.getAlto())
                .orden(request.getOrden())
                .activo(true)
                .build();

        return convertirAResponse(poolRepository.save(pool));
    }

    @Transactional(readOnly = true)
    public List<PoolResponseDto> listarPorProceso(Long procesoId) {
        return poolRepository.findByProcesoIdAndActivoTrue(procesoId)
                .stream()
                .map(this::convertirAResponse)
                .toList();
    }
    @Transactional
public void retirar(Long poolId, Long procesoId) {

    Pool pool = poolRepository
            .findByIdAndProcesoIdAndActivoTrue(poolId, procesoId)
            .orElseThrow(() -> new EntityNotFoundException(
                    "Pool no encontrado o no pertenece al proceso"));

    if (Boolean.TRUE.equals(pool.getPropietario())) {
        throw new IllegalStateException(
                "No se puede retirar el pool propietario del proceso");
    }

    if (laneRepository.existsByPoolIdAndActivoTrue(poolId)) {
        throw new IllegalStateException(
                "No se puede retirar el pool porque tiene lanes activas");
    }

    pool.setActivo(false);
    poolRepository.save(pool);
}

    private PoolResponseDto convertirAResponse(Pool pool) {
        return new PoolResponseDto(
                pool.getId(),
                pool.getProceso().getId(),
                pool.getNombre(),
                pool.getTipoParticipante(),
                pool.getPropietario(),
                pool.getCajaNegra(),
                pool.getPosicionX(),
                pool.getPosicionY(),
                pool.getAncho(),
                pool.getAlto(),
                pool.getOrden(),
                pool.getActivo()
        );
    }
}    

