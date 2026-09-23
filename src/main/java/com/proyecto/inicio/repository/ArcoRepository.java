package com.proyecto.inicio.repository;

import com.proyecto.inicio.entity.Arco;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ArcoRepository extends JpaRepository<Arco, Long> {
    boolean existsByOrigenIdAndDestinoId(Long origenId, Long destinoId);
    boolean existsByDestinoIdAndActivoTrue(Long destinoId);
    void deleteByOrigenIdOrDestinoId(Long origenId, Long destinoId);
}
