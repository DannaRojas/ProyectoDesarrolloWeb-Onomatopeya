package com.proyecto.inicio.repository;

import com.proyecto.inicio.entity.PermisoEstructura;
import com.proyecto.inicio.entity.enums.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface PermisoEstructuraRepository extends JpaRepository<PermisoEstructura, Long> {
    @Query("select p from PermisoEstructura p where p.proceso.id = :procesoId order by p.rolAcceso, p.recurso, p.accion")
    List<PermisoEstructura> listarPorProceso(@Param("procesoId") Long procesoId);

    @Query("select p from PermisoEstructura p where p.proceso.id = :procesoId and p.rolAcceso = :rol and p.recurso = :recurso and p.accion = :accion")
    Optional<PermisoEstructura> buscarRegla(@Param("procesoId") Long procesoId, @Param("rol") RolAcceso rol, @Param("recurso") RecursoEstructura recurso, @Param("accion") AccionEstructura accion);
}
