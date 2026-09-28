package com.proyecto.inicio.repository;

import com.proyecto.inicio.entity.Proceso;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import com.proyecto.inicio.entity.enums.EstadoPublicacion;

import java.util.List;
import java.util.Optional;

public interface ProcesoRepository extends JpaRepository<Proceso, Long> {
    @Query("select (count(p) > 0) from Proceso p where p.empresa.id = :empresaId and lower(p.nombre) = lower(:nombre) and (:excluir is null or p.id <> :excluir)")
    boolean nombreOcupado(@Param("empresaId") Long empresaId, @Param("nombre") String nombre, @Param("excluir") Long excluir);

    @Query(value = """
            select p from Proceso p join fetch p.empresa join fetch p.creador
            where (p.empresa.id = :empresaId or (p.activo = true and exists
                (select a.id from AccesoProceso a where a.proceso = p and a.empresaInvitada.id = :empresaId and a.activo = true)))
            and p.empresa.activo = true and p.activo = :activo
            and (:nombre = '' or lower(p.nombre) like lower(concat('%', :nombre, '%')))
            and (:categoria = '' or lower(p.categoria) = lower(:categoria))
            and (:estado is null or p.estadoPublicacion = :estado)
            """, countQuery = """
            select count(p) from Proceso p where
            (p.empresa.id = :empresaId or (p.activo = true and exists
                (select a.id from AccesoProceso a where a.proceso = p and a.empresaInvitada.id = :empresaId and a.activo = true)))
            and p.empresa.activo = true and p.activo = :activo
            and (:nombre = '' or lower(p.nombre) like lower(concat('%', :nombre, '%')))
            and (:categoria = '' or lower(p.categoria) = lower(:categoria))
            and (:estado is null or p.estadoPublicacion = :estado)
            """)
    Page<Proceso> buscar(@Param("empresaId") Long empresaId, @Param("nombre") String nombre,
            @Param("categoria") String categoria, @Param("estado") EstadoPublicacion estado,
            @Param("activo") boolean activo, Pageable pagina);

    boolean existsByEmpresaIdAndNombre(Long empresaId, String nombre);

    List<Proceso> findByEmpresaIdAndActivoTrue(Long empresaId);

    Optional<Proceso> findByIdAndEmpresaIdAndActivoTrue(Long id, Long empresaId);
}    

