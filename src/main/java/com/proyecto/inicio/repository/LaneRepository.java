package com.proyecto.inicio.repository;

import com.proyecto.inicio.entity.Lane;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface LaneRepository extends JpaRepository<Lane, Long> {
    boolean existsByPoolIdAndActivoTrue(Long poolId);
    boolean existsByRolProcesoIdAndActivoTrue(Long rolProcesoId);
    List<Lane> findByPoolIdAndActivoTrueOrderByOrden(Long poolId);
    Optional<Lane> findByIdAndPoolIdAndActivoTrue(Long id, Long poolId);
}
