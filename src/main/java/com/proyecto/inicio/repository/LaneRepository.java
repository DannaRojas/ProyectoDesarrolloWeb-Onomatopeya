package com.proyecto.inicio.repository;

import com.proyecto.inicio.entity.Lane;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LaneRepository extends JpaRepository<Lane, Long> {
    @Query("select distinct l.pool.proceso.id from Lane l where l.rolProceso.id = :rolId and l.activo = true order by l.pool.proceso.id")
    List<Long> procesosEnUso(@Param("rolId") Long rolId);

    @Query("select l from Lane l where l.pool.id = :poolId order by l.orden")
    List<Lane> listarTodas(@Param("poolId") Long poolId);
    boolean existsByPoolIdAndActivoTrue(Long poolId);
    boolean existsByRolProcesoIdAndActivoTrue(Long rolProcesoId);
    List<Lane> findByPoolIdAndActivoTrueOrderByOrden(Long poolId);
    Optional<Lane> findByIdAndPoolIdAndActivoTrue(Long id, Long poolId);
}
