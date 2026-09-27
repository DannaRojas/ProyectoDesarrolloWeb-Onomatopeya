package com.proyecto.inicio.repository;

import com.proyecto.inicio.entity.Mensaje;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface MensajeRepository extends JpaRepository<Mensaje, Long> {
    @Query(name = "Mensaje.listarActivosPorProceso")
    List<Mensaje> listarActivosPorProceso(@Param("procesoId") Long procesoId);

    @Query("select m from Mensaje m where m.id = :id and m.nodo.proceso.id = :procesoId and m.activo = true")
    Optional<Mensaje> buscarActivo(@Param("id") Long id, @Param("procesoId") Long procesoId);

    @Query("select m from Mensaje m where m.nodo.id = :nodoId")
    Optional<Mensaje> buscarPorNodo(@Param("nodoId") Long nodoId);
}
