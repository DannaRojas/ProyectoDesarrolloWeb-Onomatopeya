package com.proyecto.inicio.repository;

import com.proyecto.inicio.entity.Empresa;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface EmpresaRepository extends JpaRepository<Empresa, Long> {
    boolean existsByNit(String nit);
    Optional<Empresa> findByNit(String nit);
}