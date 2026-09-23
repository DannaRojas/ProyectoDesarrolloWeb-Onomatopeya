package com.proyecto.inicio.repository;

import com.proyecto.inicio.entity.Nodo;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NodoRepository extends JpaRepository<Nodo, Long> {
    boolean existsByIdAndProcesoIdAndPoolIdAndEstadoNot(Long id, Long procesoId, Long poolId, com.proyecto.inicio.entity.enums.EstadoNodo estado);
}
