package com.proyecto.inicio.service;

import com.proyecto.inicio.dto.response.UsuarioResponseDto;
import com.proyecto.inicio.dto.UsuarioDto.CambiarRol;
import com.proyecto.inicio.dto.UsuarioDto.Desactivar;
import com.proyecto.inicio.entity.Proceso;
import com.proyecto.inicio.entity.HistorialCambio;
import com.proyecto.inicio.entity.enums.AccionHistorial;
import com.proyecto.inicio.entity.enums.TipoActor;
import com.proyecto.inicio.exception.AccesoColaboracionException;
import com.proyecto.inicio.entity.Empresa;
import com.proyecto.inicio.entity.Usuario;
import com.proyecto.inicio.entity.enums.EstadoUsuario;
import com.proyecto.inicio.entity.enums.RolAcceso;
import com.proyecto.inicio.repository.EmpresaRepository;
import com.proyecto.inicio.repository.UsuarioRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.util.UUID;
import static org.assertj.core.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE, properties = {
        "spring.datasource.url=jdbc:h2:mem:pruebas-consulta-usuario;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa", "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.properties.hibernate.hbm2ddl.halt_on_error=true"
})
@Transactional
class UsuarioConsultaServiceTest {
    @Autowired UsuarioService servicio;
    @Autowired EmpresaRepository empresas;
    @Autowired UsuarioRepository usuarios;
    @Autowired EntityManager em;
    Empresa empresa;
    Empresa otraEmpresa;

    @BeforeEach
    void preparar() {
        empresa = crearEmpresa("Empresa de prueba");
        otraEmpresa = crearEmpresa("Otra empresa");
    }

    @Test
    void listaSoloLaEmpresaIndicadaEnOrdenYConTodosLosEstados() {
        Usuario z = crearUsuario(empresa, "Zoe", EstadoUsuario.ACTIVO);
        Usuario a = crearUsuario(empresa, "Ana", EstadoUsuario.INVITADO);
        Usuario b = crearUsuario(empresa, "Ana", EstadoUsuario.INACTIVO);
        crearUsuario(otraEmpresa, "Usuario ajeno", EstadoUsuario.ACTIVO);
        em.flush();
        em.clear();

        var resultado = servicio.listarPorEmpresa(empresa.getId());

        assertThat(resultado).extracting(UsuarioResponseDto::getId)
                .containsExactly(a.getId(), b.getId(), z.getId());
        assertThat(resultado).allSatisfy(u -> assertThat(u.getEmpresaId()).isEqualTo(empresa.getId()));
        assertThat(resultado).extracting(UsuarioResponseDto::getEstado)
                .containsExactly(EstadoUsuario.INVITADO, EstadoUsuario.INACTIVO, EstadoUsuario.ACTIVO);
    }

    @Test
    void devuelveListaVaciaCuandoLaEmpresaNoTieneUsuarios() {
        assertThat(servicio.listarPorEmpresa(empresa.getId())).isEmpty();
    }

    @Test
    void empresaInexistenteNoSeConfundeConListaVacia() {
        assertThatThrownBy(() -> servicio.listarPorEmpresa(Long.MAX_VALUE))
                .isInstanceOf(EntityNotFoundException.class).hasMessage("Empresa no encontrada");
        assertThatThrownBy(() -> servicio.consultar(Long.MAX_VALUE, 1L))
                .isInstanceOf(EntityNotFoundException.class).hasMessage("Empresa no encontrada");
    }

    @Test
    void consultaUnUsuarioYMapeaSuEmpresaSinExponerSecretos() {
        Usuario usuario = crearUsuario(empresa, "Usuario de prueba", EstadoUsuario.ACTIVO);
        usuario.setPasswordHash("hash-de-prueba-no-publicar");
        usuario.setTokenInvitacionHash("token-de-prueba-no-publicar");
        usuario.setInvitadoPor(crearUsuario(empresa, "Invitador", EstadoUsuario.ACTIVO));
        em.flush();
        em.clear();

        var resultado = servicio.consultar(empresa.getId(), usuario.getId());
        assertThat(resultado.getId()).isEqualTo(usuario.getId());
        assertThat(resultado.getEmpresaId()).isEqualTo(empresa.getId());
        assertThat(resultado.getNombre()).isEqualTo("Usuario de prueba");
        assertThat(resultado.getCorreo()).isEqualTo(usuario.getCorreo());
        assertThat(resultado.getRolAcceso()).isEqualTo(RolAcceso.LECTURA);
        assertThat(resultado.getEstado()).isEqualTo(EstadoUsuario.ACTIVO);
        assertThat(resultado.getFechaCreacion()).isNotNull();
        assertThat(resultado.getVersion()).isEqualTo(usuario.getVersion());
        var json = new ObjectMapper().valueToTree(resultado);
        assertThat(json.propertyNames()).containsExactlyInAnyOrder(
                "id", "empresaId", "nombre", "correo", "rolAcceso", "estado", "fechaCreacion", "version");
        assertThat(json.toString()).doesNotContain("passwordHash", "tokenInvitacionHash",
                "hash-de-prueba-no-publicar", "token-de-prueba-no-publicar", "invitadoPor");
    }

    @Test
    void unUsuarioDeOtraEmpresaSeRespondeComoNoEncontrado() {
        Usuario ajeno = crearUsuario(otraEmpresa, "Ajeno", EstadoUsuario.ACTIVO);
        assertThatThrownBy(() -> servicio.consultar(empresa.getId(), ajeno.getId()))
                .isInstanceOf(EntityNotFoundException.class).hasMessage("Usuario no encontrado en la empresa.");
        assertThatThrownBy(() -> servicio.consultar(empresa.getId(), Long.MAX_VALUE))
                .isInstanceOf(EntityNotFoundException.class).hasMessage("Usuario no encontrado en la empresa.");
    }

    @Test
    void rechazaIdentificadoresNulosCeroONegativos() {
        for (Long invalido : new Long[]{null, 0L, -1L}) {
            assertThatThrownBy(() -> servicio.listarPorEmpresa(invalido))
                    .isInstanceOf(ConstraintViolationException.class);
            assertThatThrownBy(() -> servicio.consultar(invalido, 1L))
                    .isInstanceOf(ConstraintViolationException.class);
            assertThatThrownBy(() -> servicio.consultar(empresa.getId(), invalido))
                    .isInstanceOf(ConstraintViolationException.class);
        }
    }

    @Test
    void consultarNoCambiaLosRegistrosNiSusVersiones() {
        Usuario usuario = crearUsuario(empresa, "Sin cambios", EstadoUsuario.ACTIVO);
        em.flush();
        long totalUsuarios = usuarios.count();
        Long version = usuario.getVersion();
        em.clear();

        servicio.listarPorEmpresa(empresa.getId());
        servicio.consultar(empresa.getId(), usuario.getId());
        em.flush();
        em.clear();

        assertThat(usuarios.count()).isEqualTo(totalUsuarios);
        assertThat(usuarios.findById(usuario.getId()).orElseThrow().getVersion()).isEqualTo(version);
        assertThat(empresas.findById(empresa.getId()).orElseThrow().getNombre()).isEqualTo("Empresa de prueba");
    }

    @Test
    void administradorCambiaRolSinAlterarEmpresaYRegistraCambio() {
        Usuario admin = administrador();
        Usuario usuario = crearUsuario(empresa, "Editor", EstadoUsuario.ACTIVO);
        var respuesta = servicio.cambiarRol(admin.getId(), usuario.getId(),
                new CambiarRol(RolAcceso.EDITOR, usuario.getVersion()));
        assertThat(respuesta.getRolAcceso()).isEqualTo(RolAcceso.EDITOR);
        assertThat(respuesta.getEmpresaId()).isEqualTo(empresa.getId());
        assertThat(respuesta.getVersion()).isEqualTo(1L);
        assertThat(em.createQuery("select h from HistorialCambio h where h.objetoId = :id", HistorialCambio.class)
                .setParameter("id", usuario.getId()).getResultList())
                .singleElement().satisfies(h -> {
                    assertThat(h.getAccion()).isEqualTo(AccionHistorial.ACTUALIZAR);
                    assertThat(h.getActor().getId()).isEqualTo(admin.getId());
                });
    }

    @Test
    void desactivarConservaProcesoEHistorialDelUsuario() {
        Usuario admin = administrador();
        Usuario usuario = crearUsuario(empresa, "Creador", EstadoUsuario.ACTIVO);
        Proceso proceso = Proceso.builder().empresa(empresa).creador(usuario).nombre("Proceso conservado")
                .activo(true).build();
        em.persist(proceso);
        HistorialCambio anterior = HistorialCambio.builder().empresaContexto(empresa).proceso(proceso)
                .actor(usuario).tipoActor(TipoActor.USUARIO).tipoObjeto("PROCESO").objetoId(proceso.getId())
                .accion(AccionHistorial.CREAR).descripcion("Creación original.").build();
        em.persist(anterior);
        em.flush();
        var respuesta = servicio.desactivar(admin.getId(), usuario.getId(),
                new Desactivar(usuario.getVersion(), true));
        em.flush();
        em.clear();
        assertThat(respuesta.getEstado()).isEqualTo(EstadoUsuario.INACTIVO);
        assertThat(usuarios.findById(usuario.getId())).isPresent();
        assertThat(em.find(Proceso.class, proceso.getId()).getActivo()).isTrue();
        assertThat(em.find(Proceso.class, proceso.getId()).getCreador().getId()).isEqualTo(usuario.getId());
        assertThat(em.find(HistorialCambio.class, anterior.getId()).getActor().getId()).isEqualTo(usuario.getId());
    }

    @Test
    void lectorEditorYAdministradorAjenoNoPuedenModificar() {
        Usuario objetivo = crearUsuario(empresa, "Objetivo", EstadoUsuario.ACTIVO);
        Usuario actor = crearUsuario(empresa, "Actor", EstadoUsuario.ACTIVO);
        for (RolAcceso rol : new RolAcceso[]{RolAcceso.LECTURA, RolAcceso.EDITOR}) {
            actor.setRolAcceso(rol);
            em.flush();
            assertThatThrownBy(() -> servicio.cambiarRol(actor.getId(), objetivo.getId(),
                    new CambiarRol(RolAcceso.EDITOR, objetivo.getVersion())))
                    .isInstanceOf(AccesoColaboracionException.class);
            assertThatThrownBy(() -> servicio.desactivar(actor.getId(), objetivo.getId(),
                    new Desactivar(objetivo.getVersion(), true))).isInstanceOf(AccesoColaboracionException.class);
        }
        Usuario ajeno = crearUsuario(otraEmpresa, "Admin ajeno", EstadoUsuario.ACTIVO);
        ajeno.setRolAcceso(RolAcceso.ADMINISTRADOR);
        em.flush();
        assertThatThrownBy(() -> servicio.cambiarRol(ajeno.getId(), objetivo.getId(),
                new CambiarRol(RolAcceso.EDITOR, objetivo.getVersion()))).isInstanceOf(EntityNotFoundException.class);
        assertThat(objetivo.getRolAcceso()).isEqualTo(RolAcceso.LECTURA);
        assertThat(objetivo.getEstado()).isEqualTo(EstadoUsuario.ACTIVO);
    }

    @Test
    void versionAntiguaYFaltaConfirmacionNoModificanUsuario() {
        Usuario admin = administrador();
        Usuario usuario = crearUsuario(empresa, "Objetivo", EstadoUsuario.ACTIVO);
        assertThatThrownBy(() -> servicio.cambiarRol(admin.getId(), usuario.getId(),
                new CambiarRol(RolAcceso.EDITOR, 99L))).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> servicio.desactivar(admin.getId(), usuario.getId(),
                new Desactivar(usuario.getVersion(), false))).isInstanceOf(ConstraintViolationException.class);
        assertThatThrownBy(() -> servicio.cambiarRol(admin.getId(), usuario.getId(),
                new CambiarRol(null, null))).isInstanceOf(ConstraintViolationException.class);
        assertThat(usuario.getEstado()).isEqualTo(EstadoUsuario.ACTIVO);
        assertThat(usuario.getRolAcceso()).isEqualTo(RolAcceso.LECTURA);
    }

    @Test
    void desactivarInvitadoAnulaLaInvitacion() {
        Usuario admin = administrador();
        Usuario invitado = crearUsuario(empresa, "Invitado", EstadoUsuario.INVITADO);
        invitado.setTokenInvitacionHash("hash-de-ejemplo");
        invitado.setInvitacionExpiraEn(java.time.OffsetDateTime.now().plusHours(1));
        em.flush();
        servicio.desactivar(admin.getId(), invitado.getId(), new Desactivar(invitado.getVersion(), true));
        assertThat(invitado.getEstado()).isEqualTo(EstadoUsuario.INACTIVO);
        assertThat(invitado.getTokenInvitacionHash()).isNull();
        assertThat(invitado.getInvitacionExpiraEn()).isNull();
    }

    @Test
    void administradorInactivoOEmpresaInactivaNoPuedenGestionar() {
        Usuario admin = administrador();
        Usuario objetivo = crearUsuario(empresa, "Objetivo", EstadoUsuario.ACTIVO);
        admin.setEstado(EstadoUsuario.INACTIVO);
        em.flush();
        assertThatThrownBy(() -> servicio.desactivar(admin.getId(), objetivo.getId(),
                new Desactivar(objetivo.getVersion(), true))).isInstanceOf(AccesoColaboracionException.class);
        admin.setEstado(EstadoUsuario.ACTIVO);
        empresa.setActivo(false);
        em.flush();
        assertThatThrownBy(() -> servicio.cambiarRol(admin.getId(), objetivo.getId(),
                new CambiarRol(RolAcceso.EDITOR, objetivo.getVersion())))
                .isInstanceOf(AccesoColaboracionException.class);
        assertThat(objetivo.getEstado()).isEqualTo(EstadoUsuario.ACTIVO);
    }

    private Usuario administrador() {
        Usuario admin = crearUsuario(empresa, "Administrador", EstadoUsuario.ACTIVO);
        admin.setRolAcceso(RolAcceso.ADMINISTRADOR);
        em.flush();
        return admin;
    }

    private Empresa crearEmpresa(String nombre) {
        return empresas.save(Empresa.builder().nombre(nombre).nit(UUID.randomUUID().toString())
                .correoContacto("contacto@example.com").activo(true).build());
    }

    private Usuario crearUsuario(Empresa empresaUsuario, String nombre, EstadoUsuario estado) {
        return usuarios.save(Usuario.builder().empresa(empresaUsuario).nombre(nombre)
                .correo(UUID.randomUUID() + "@example.com")
                .rolAcceso(RolAcceso.LECTURA).estado(estado).build());
    }
}
