package com.proyecto.inicio.repository;

import com.proyecto.inicio.entity.Empresa;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EmpresaRepository extends JpaRepository<Empresa, Long> {

    boolean existsByNit(String nit);
}
