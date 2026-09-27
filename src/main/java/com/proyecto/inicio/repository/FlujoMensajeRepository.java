package com.proyecto.inicio.repository;

import com.proyecto.inicio.entity.FlujoMensaje;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface FlujoMensajeRepository extends JpaRepository<FlujoMensaje, Long> {
    @Query("select f from FlujoMensaje f where f.proceso.id = :procesoId and f.activo = true order by f.id")
    List<FlujoMensaje> listarActivos(@Param("procesoId") Long procesoId);

    @Query("select f from FlujoMensaje f where f.id = :id and f.proceso.id = :procesoId and f.activo = true")
    Optional<FlujoMensaje> buscarActivo(@Param("id") Long id, @Param("procesoId") Long procesoId);

    @Query("select f from FlujoMensaje f where f.activo = true and (f.nodoOrigen.id = :nodoId or f.nodoDestino.id = :nodoId)")
    List<FlujoMensaje> buscarConectados(@Param("nodoId") Long nodoId);
}
