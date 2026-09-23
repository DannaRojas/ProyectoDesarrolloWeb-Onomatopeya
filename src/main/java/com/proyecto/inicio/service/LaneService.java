package com.proyecto.inicio.service;

import com.proyecto.inicio.dto.request.LaneRequestDto;
import com.proyecto.inicio.dto.response.LaneResponseDto;
import com.proyecto.inicio.entity.Lane;
import com.proyecto.inicio.entity.Pool;
import com.proyecto.inicio.entity.RolProceso;
import com.proyecto.inicio.repository.LaneRepository;
import com.proyecto.inicio.repository.PoolRepository;
import com.proyecto.inicio.repository.RolProcesoRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@RequiredArgsConstructor
public class LaneService {
    private final LaneRepository laneRepository;
    private final PoolRepository poolRepository;
    private final RolProcesoRepository rolProcesoRepository;

    @Transactional
    public LaneResponseDto crear(LaneRequestDto request) {
        Pool pool = poolRepository.findById(request.getPoolId())
                .orElseThrow(() -> new EntityNotFoundException("Pool no encontrado"));
        RolProceso rol = rolProcesoRepository.findById(request.getRolProcesoId())
                .orElseThrow(() -> new EntityNotFoundException("Rol de proceso no encontrado"));
        if (!pool.getProceso().getEmpresa().getId().equals(rol.getEmpresa().getId()))
            throw new IllegalArgumentException("La lane y su rol deben pertenecer a la misma empresa");
        Lane lane = Lane.builder().pool(pool).rolProceso(rol).orden(request.getOrden())
                .altura(request.getAltura()).activo(true).build();
        return convertir(laneRepository.save(lane));
    }

    @Transactional(readOnly = true)
    public List<LaneResponseDto> listarPorPool(Long poolId) {
        return laneRepository.findByPoolIdAndActivoTrueOrderByOrden(poolId).stream().map(this::convertir).toList();
    }

    @Transactional
    public void retirar(Long laneId, Long poolId) {
        Lane lane = laneRepository.findByIdAndPoolIdAndActivoTrue(laneId, poolId)
                .orElseThrow(() -> new EntityNotFoundException("Lane no encontrada en el pool"));
        lane.setActivo(false);
        laneRepository.save(lane);
    }

    private LaneResponseDto convertir(Lane lane) {
        return new LaneResponseDto(lane.getId(), lane.getPool().getId(), lane.getRolProceso().getId(),
                lane.getRolProceso().getNombre(), lane.getOrden(), lane.getAltura(), lane.getActivo());
    }
}
