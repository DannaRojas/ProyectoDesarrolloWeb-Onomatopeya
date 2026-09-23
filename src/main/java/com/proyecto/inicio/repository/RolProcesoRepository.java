package com.proyecto.inicio.repository;

import com.proyecto.inicio.entity.RolProceso;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RolProcesoRepository extends JpaRepository<RolProceso, Long> {

    boolean existsByEmpresaIdAndNombre(Long empresaId, String nombre);

    List<RolProceso> findByEmpresaIdAndActivoTrue(Long empresaId);

    Optional<RolProceso> findByIdAndEmpresaIdAndActivoTrue(
            Long id,
            Long empresaId
    );
}