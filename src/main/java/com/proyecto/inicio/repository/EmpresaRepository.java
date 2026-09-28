package com.proyecto.inicio.repository;

import com.proyecto.inicio.entity.Empresa;
import org.springframework.data.jpa.repository.JpaRepository;
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
}
