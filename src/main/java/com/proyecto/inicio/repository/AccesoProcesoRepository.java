package com.proyecto.inicio.repository;

import com.proyecto.inicio.entity.AccesoProceso;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface AccesoProcesoRepository extends JpaRepository<AccesoProceso, Long> {
    Optional<AccesoProceso> findByProcesoIdAndEmpresaInvitadaIdAndActivoTrue(Long procesoId, Long empresaInvitadaId);
}
