package com.proyecto.inicio.repository;

import com.proyecto.inicio.entity.CampoMensaje;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface CampoMensajeRepository extends JpaRepository<CampoMensaje, Long> {
    @Query("select c from CampoMensaje c where c.mensaje.id = :mensajeId and c.activo = true order by c.orden, c.id")
    List<CampoMensaje> listarActivos(@Param("mensajeId") Long mensajeId);

    @Query("select c from CampoMensaje c where c.id = :id and c.mensaje.id = :mensajeId and c.activo = true")
    Optional<CampoMensaje> buscarActivo(@Param("id") Long id, @Param("mensajeId") Long mensajeId);

    @Query("select (count(c) > 0) from CampoMensaje c where c.mensaje.id = :mensajeId and c.nombre = :nombre and (:excluirId is null or c.id <> :excluirId)")
    boolean nombreOcupado(@Param("mensajeId") Long mensajeId, @Param("nombre") String nombre, @Param("excluirId") Long excluirId);
}
