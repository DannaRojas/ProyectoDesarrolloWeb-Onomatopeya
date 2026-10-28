package com.proyecto.inicio.repository;

import com.proyecto.inicio.entity.Usuario;
import com.proyecto.inicio.entity.enums.EstadoUsuario;
import com.proyecto.inicio.entity.enums.RolAcceso;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
    boolean existsByCorreo(String correo);

    Optional<Usuario> findByCorreo(String correo);

    @Query("select u from Usuario u join fetch u.empresa where u.correo = :correo")
    Optional<Usuario> buscarPorCorreoConEmpresa(@Param("correo") String correo);

    @Query("select case when count(u) > 0 then true else false end from Usuario u where lower(trim(u.correo)) = :correo")
    boolean existeCorreoNormalizado(@Param("correo") String correo);

    @Query("select u from Usuario u join fetch u.empresa where u.id = :usuarioId")
    Optional<Usuario> buscarConEmpresa(@Param("usuarioId") Long usuarioId);

    @Query("select u from Usuario u join fetch u.empresa where u.tokenInvitacionHash = :hash")
    Optional<Usuario> buscarPorInvitacion(@Param("hash") String hash);

    @Query("select u from Usuario u join fetch u.empresa where u.empresa.id = :empresaId order by u.nombre, u.id")
    List<Usuario> listarPorEmpresa(@Param("empresaId") Long empresaId);

    @Query("select u from Usuario u join fetch u.empresa where u.empresa.id = :empresaId and u.id = :usuarioId")
    Optional<Usuario> buscarEnEmpresa(
            @Param("empresaId") Long empresaId, @Param("usuarioId") Long usuarioId);

    @Query("select u.empresa.id from Usuario u where u.id = :usuarioId")
    Optional<Long> empresaIdDeUsuario(@Param("usuarioId") Long usuarioId);

    @Query("select count(u) from Usuario u where u.empresa.id = :empresaId and u.estado = :estado and u.rolAcceso = :rol")
    long contarPorEmpresaEstadoYRol(
            @Param("empresaId") Long empresaId,
            @Param("estado") EstadoUsuario estado,
            @Param("rol") RolAcceso rol);
}
