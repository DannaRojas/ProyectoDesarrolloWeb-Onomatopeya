package com.proyecto.inicio.repository;

import com.proyecto.inicio.entity.Empresa;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface EmpresaRepository extends JpaRepository<Empresa, Long> {
    @Query("select (count(e) > 0) from Empresa e where e.nit = :nit")
    boolean existsByNit(@Param("nit") String nit);

    @Query("select e from Empresa e where e.nit = :nit")
    Optional<Empresa> findByNit(@Param("nit") String nit);

    @Query("select e from Empresa e where e.activo = :activo order by e.id")
    List<Empresa> buscarPorEstado(@Param("activo") boolean activo);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select e from Empresa e where e.id = :id")
    Optional<Empresa> bloquear(@Param("id") Long id);
}
