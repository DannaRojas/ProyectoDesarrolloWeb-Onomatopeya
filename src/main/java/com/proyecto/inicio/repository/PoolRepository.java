package com.proyecto.inicio.repository;

import com.proyecto.inicio.entity.Pool;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PoolRepository extends JpaRepository<Pool, Long> {

    boolean existsByProcesoIdAndPropietarioTrueAndActivoTrue(Long procesoId);
}