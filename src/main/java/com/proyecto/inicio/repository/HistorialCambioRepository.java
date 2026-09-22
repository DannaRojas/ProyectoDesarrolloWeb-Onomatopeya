package com.proyecto.inicio.repository;

import com.proyecto.inicio.entity.HistorialCambio;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface HistorialCambioRepository extends JpaRepository<HistorialCambio, Long> {
    List<HistorialCambio> findByEmpresaContextoId(Long empresaId);
}
