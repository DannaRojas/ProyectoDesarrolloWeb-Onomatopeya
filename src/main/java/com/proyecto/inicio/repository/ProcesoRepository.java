package com.proyecto.inicio.repository;

import com.proyecto.inicio.entity.Proceso;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProcesoRepository extends JpaRepository<Proceso, Long> {

    boolean existsByEmpresaIdAndNombre(Long empresaId, String nombre);

    List<Proceso> findByEmpresaIdAndActivoTrue(Long empresaId);

    Optional<Proceso> findByIdAndEmpresaIdAndActivoTrue(Long id, Long empresaId);
}    

