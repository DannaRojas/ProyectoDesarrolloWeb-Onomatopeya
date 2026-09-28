package com.proyecto.inicio.repository;

import com.proyecto.inicio.entity.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

// Consultas de integración, sin cambiar el repositorio que preparó Laura.
public interface UsuarioConsultaRepository extends JpaRepository<Usuario, Long> {
    @Query("select u from Usuario u join fetch u.empresa where u.id = :usuarioId")
    Optional<Usuario> buscarConEmpresa(@Param("usuarioId") Long usuarioId);

    @Query("select case when count(u) > 0 then true else false end from Usuario u where lower(trim(u.correo)) = :correo")
    boolean existeCorreoNormalizado(@Param("correo") String correo);

    @Query("select u from Usuario u join fetch u.empresa where u.empresa.id = :empresaId order by u.nombre, u.id")
    List<Usuario> listarPorEmpresa(@Param("empresaId") Long empresaId);

    @Query("select u from Usuario u join fetch u.empresa where u.empresa.id = :empresaId and u.id = :usuarioId")
    Optional<Usuario> buscarEnEmpresa(@Param("empresaId") Long empresaId, @Param("usuarioId") Long usuarioId);
}
