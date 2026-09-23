package com.proyecto.inicio.repository;

import com.proyecto.inicio.entity.Actividad;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ActividadRepository extends JpaRepository<Actividad, Long> {
    boolean existsByProcesoIdAndNombreIgnoreCase(Long procesoId, String nombre);
    boolean existsByProcesoIdAndNombreIgnoreCaseAndIdNot(Long procesoId, String nombre, Long id);
}
