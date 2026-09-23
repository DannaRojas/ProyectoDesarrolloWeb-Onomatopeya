package com.proyecto.inicio.repository;

import com.proyecto.inicio.entity.Pool;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface PoolRepository extends JpaRepository<Pool, Long> {

    boolean existsByProcesoIdAndPropietarioTrueAndActivoTrue(Long procesoId);
    List<Pool> findByProcesoIdAndActivoTrue(Long procesoId);
    Optional<Pool> findByIdAndProcesoIdAndActivoTrue(Long id, Long procesoId);
}
