package com.proyecto.inicio.repository;

import com.proyecto.inicio.entity.RolProceso;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface RolProcesoRepository extends JpaRepository<RolProceso, Long> {
    @Query("select (count(r) > 0) from RolProceso r where r.empresa.id = :empresaId and lower(r.nombre) = lower(:nombre) and (:excluir is null or r.id <> :excluir)")
    boolean nombreOcupado(@Param("empresaId") Long empresaId, @Param("nombre") String nombre, @Param("excluir") Long excluir);

    @Query("select r from RolProceso r where r.empresa.id = :empresaId and r.activo = true and lower(r.nombre) like lower(concat('%', :nombre, '%'))")
    Page<RolProceso> buscar(@Param("empresaId") Long empresaId, @Param("nombre") String nombre, Pageable pagina);

    boolean existsByEmpresaIdAndNombre(Long empresaId, String nombre);

    List<RolProceso> findByEmpresaIdAndActivoTrue(Long empresaId);

    Optional<RolProceso> findByIdAndEmpresaIdAndActivoTrue(
            Long id,
            Long empresaId
    );
}
