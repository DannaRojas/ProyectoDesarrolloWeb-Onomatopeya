package com.proyecto.inicio.repository;

import com.proyecto.inicio.entity.Empresa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface EmpresaRepository extends JpaRepository<Empresa, Long> {

    // JPQL consulta la entidad y sus atributos, no los nombres de las columnas.
    @Query("select (count(e) > 0) from Empresa e where e.nit = :nit")
    boolean existsByNit(@Param("nit") String nit);

    @Query("select e from Empresa e where e.activo = :activo order by e.id")
    List<Empresa> buscarPorEstado(@Param("activo") boolean activo);

    @Query("select e from Empresa e where e.id = :id and e.activo = true")
    Optional<Empresa> buscarActivaPorId(@Param("id") Long id);
}
