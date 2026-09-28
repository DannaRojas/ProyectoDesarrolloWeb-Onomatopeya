package com.proyecto.inicio.repository;

import com.proyecto.inicio.entity.Nodo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface NodoRepository extends JpaRepository<Nodo, Long> {
    @Query("select n from Nodo n where n.proceso.id = :procesoId and n.estado <> com.proyecto.inicio.entity.enums.EstadoNodo.RETIRADO order by n.id")
    List<Nodo> listarVigentes(@Param("procesoId") Long procesoId);

    @Query("select (count(n) > 0) from Nodo n where n.pool.id = :poolId and n.estado <> com.proyecto.inicio.entity.enums.EstadoNodo.RETIRADO")
    boolean poolEnUso(@Param("poolId") Long poolId);
    boolean existsByIdAndProcesoIdAndPoolIdAndEstadoNot(Long id, Long procesoId, Long poolId, com.proyecto.inicio.entity.enums.EstadoNodo estado);
}
