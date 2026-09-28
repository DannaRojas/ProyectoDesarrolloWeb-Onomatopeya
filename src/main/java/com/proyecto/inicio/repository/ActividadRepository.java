package com.proyecto.inicio.repository;

import com.proyecto.inicio.entity.Actividad;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ActividadRepository extends JpaRepository<Actividad, Long> {
    @Query("select (count(a) > 0) from Actividad a where a.lane.id = :laneId and a.estado <> com.proyecto.inicio.entity.enums.EstadoNodo.RETIRADO")
    boolean laneEnUso(@Param("laneId") Long laneId);

    @Query("select (count(a) > 0) from Actividad a where a.lane.rolProceso.id = :rolId and a.estado <> com.proyecto.inicio.entity.enums.EstadoNodo.RETIRADO")
    boolean rolEnUso(@Param("rolId") Long rolId);
    boolean existsByProcesoIdAndNombreIgnoreCase(Long procesoId, String nombre);
    boolean existsByProcesoIdAndNombreIgnoreCaseAndIdNot(Long procesoId, String nombre, Long id);
}
