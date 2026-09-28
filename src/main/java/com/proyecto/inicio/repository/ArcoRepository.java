package com.proyecto.inicio.repository;

import com.proyecto.inicio.entity.Arco;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface ArcoRepository extends JpaRepository<Arco, Long> {
    boolean existsByOrigenIdAndDestinoId(Long origenId, Long destinoId);
    boolean existsByDestinoIdAndActivoTrue(Long destinoId);
    @Query("select a from Arco a where a.activo = true and (a.origen.id = :nodoId or a.destino.id = :nodoId)")
    List<Arco> conectados(@Param("nodoId") Long nodoId);

    @Query("select a from Arco a where a.activo = true and a.origen.id = :nodoId order by a.id")
    List<Arco> salientes(@Param("nodoId") Long nodoId);

    @Query("select a from Arco a where a.activo = true and a.proceso.id = :procesoId order by a.id")
    List<Arco> listarActivos(@Param("procesoId") Long procesoId);

    @Query("select a from Arco a where a.id = :id and a.proceso.id = :procesoId and a.activo = true")
    Optional<Arco> buscarActivo(@Param("id") Long id, @Param("procesoId") Long procesoId);

    @Query("select a from Arco a where a.origen.id = :origen and a.destino.id = :destino")
    Optional<Arco> buscarPar(@Param("origen") Long origen, @Param("destino") Long destino);
}
