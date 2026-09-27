package com.proyecto.inicio.repository;

import com.proyecto.inicio.entity.UsoMensajeActividad;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface UsoMensajeActividadRepository extends JpaRepository<UsoMensajeActividad, Long> {
    @Query("select u from UsoMensajeActividad u where u.mensaje.id = :mensajeId and u.activo = true order by u.id")
    List<UsoMensajeActividad> listarActivos(@Param("mensajeId") Long mensajeId);

    @Query("select u from UsoMensajeActividad u where u.mensaje.id = :mensajeId and u.actividad.id = :actividadId")
    Optional<UsoMensajeActividad> buscarRelacion(@Param("mensajeId") Long mensajeId, @Param("actividadId") Long actividadId);

    @Query("select u from UsoMensajeActividad u where u.id = :id and u.mensaje.id = :mensajeId and u.activo = true")
    Optional<UsoMensajeActividad> buscarActivo(@Param("id") Long id, @Param("mensajeId") Long mensajeId);
}
