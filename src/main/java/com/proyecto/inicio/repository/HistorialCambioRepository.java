package com.proyecto.inicio.repository;

import com.proyecto.inicio.entity.HistorialCambio;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface HistorialCambioRepository extends JpaRepository<HistorialCambio, Long> {
    @Query("select h from HistorialCambio h left join fetch h.actor where h.proceso.id = :procesoId order by h.fecha, h.id")
    List<HistorialCambio> listarPorProceso(@Param("procesoId") Long procesoId);
    List<HistorialCambio> findByEmpresaContextoId(Long empresaId);
}
