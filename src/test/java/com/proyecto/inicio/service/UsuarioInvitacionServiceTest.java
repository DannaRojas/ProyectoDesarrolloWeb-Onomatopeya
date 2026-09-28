package com.proyecto.inicio.service;

import com.proyecto.inicio.dto.request.InvitarUsuarioRequestDto;
import com.proyecto.inicio.entity.Empresa;
import com.proyecto.inicio.entity.Usuario;
import com.proyecto.inicio.entity.enums.EstadoUsuario;
import com.proyecto.inicio.entity.enums.RolAcceso;
import com.proyecto.inicio.exception.AccesoColaboracionException;
import com.proyecto.inicio.repository.EmpresaRepository;
import com.proyecto.inicio.repository.UsuarioRepository;
import jakarta.persistence.EntityManager;
import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.HexFormat;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE, properties = {
        "spring.datasource.url=jdbc:h2:mem:pruebas-invitacion-usuario;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa", "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.properties.hibernate.hbm2ddl.halt_on_error=true"
})
@Transactional
class UsuarioInvitacionServiceTest {
    @Autowired UsuarioInvitacionService servicio;
    @Autowired UsuarioConsultaService consultas;
    @Autowired EmpresaRepository empresas;
    @Autowired UsuarioRepository usuarios;
    @Autowired EntityManager em;
    Empresa empresa;
    Usuario admin;

    @BeforeEach
    void preparar() {
        empresa = empresas.save(Empresa.builder().nombre("Empresa").nit(UUID.randomUUID().toString())
                .correoContacto("contacto@example.com").activo(true).build());
        admin = usuarios.save(Usuario.builder().empresa(empresa).nombre("Administrador")
                .correo(UUID.randomUUID() + "@example.com")
                .rolAcceso(RolAcceso.ADMINISTRADOR).estado(EstadoUsuario.ACTIVO).build());
    }

    @Test
    void administradorPreparaInvitadoConEmpresaRolHashYVencimiento() throws Exception {
        var datos = datos("Persona@Example.com", RolAcceso.EDITOR);
        datos.setNombre("  Persona invitada  ");
        // Comparar con la misma precisión que el servicio y PostgreSQL.
        OffsetDateTime antes = OffsetDateTime.now(ZoneOffset.UTC).truncatedTo(ChronoUnit.MICROS);
        var respuesta = servicio.invitar(admin.getId(), datos);
        em.clear();
        Usuario guardado = usuarios.findById(respuesta.getUsuario().getId()).orElseThrow();

        assertThat(guardado.getEmpresa().getId()).isEqualTo(empresa.getId());
        assertThat(guardado.getInvitadoPor().getId()).isEqualTo(admin.getId());
        assertThat(guardado.getNombre()).isEqualTo("Persona invitada");
        assertThat(guardado.getCorreo()).isEqualTo("persona@example.com");
        assertThat(guardado.getRolAcceso()).isEqualTo(RolAcceso.EDITOR);
        assertThat(guardado.getEstado()).isEqualTo(EstadoUsuario.INVITADO);
        assertThat(guardado.getPasswordHash()).isNull();
        assertThat(guardado.getFechaCreacion()).isNotNull();
        assertThat(guardado.getVersion()).isZero();
        String token = respuesta.getTokenInvitacion();
        assertThat(Base64.getUrlDecoder().decode(token)).hasSize(32);
        assertThat(guardado.getTokenInvitacionHash()).isEqualTo(HexFormat.of().formatHex(
                MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8))));
        assertThat(guardado.getTokenInvitacionHash()).isNotEqualTo(token);
        assertThat(respuesta.getExpiraEn()).isBetween(antes.plusHours(24),
                OffsetDateTime.now(ZoneOffset.UTC).plusHours(24).truncatedTo(ChronoUnit.MICROS));
        assertThat(guardado.getInvitacionExpiraEn().toInstant()).isEqualTo(respuesta.getExpiraEn().toInstant());
        assertThat(respuesta.getUsuario().getEmpresaId()).isEqualTo(empresa.getId());
        assertThat(consultas.listarPorEmpresa(empresa.getId())).hasSize(2);
    }

    @Test
    void permiteLosTresRolesPeroNingunInvitadoQuedaActivo() {
        for (RolAcceso rol : RolAcceso.values()) {
            var respuesta = servicio.invitar(admin.getId(), datos(UUID.randomUUID() + "@example.com", rol));
            assertThat(respuesta.getUsuario().getRolAcceso()).isEqualTo(rol);
            assertThat(respuesta.getUsuario().getEstado()).isEqualTo(EstadoUsuario.INVITADO);
        }
    }

    @Test
    void rechazaEditorYLectorSinCrearUsuarios() {
        long total = usuarios.count();
        for (RolAcceso rol : new RolAcceso[]{RolAcceso.EDITOR, RolAcceso.LECTURA}) {
            admin.setRolAcceso(rol);
            em.flush();
            assertThatThrownBy(() -> servicio.invitar(admin.getId(), datos("nuevo@example.com", RolAcceso.LECTURA)))
                    .isInstanceOf(AccesoColaboracionException.class);
        }
        assertThat(usuarios.count()).isEqualTo(total);
    }

    @Test
    void rechazaAdministradorInvitadoOInactivoYEmpresaInactiva() {
        for (EstadoUsuario estado : new EstadoUsuario[]{EstadoUsuario.INVITADO, EstadoUsuario.INACTIVO}) {
            admin.setEstado(estado);
            em.flush();
            assertThatThrownBy(() -> servicio.invitar(admin.getId(), datos("nuevo@example.com", RolAcceso.LECTURA)))
                    .isInstanceOf(AccesoColaboracionException.class);
        }
        admin.setEstado(EstadoUsuario.ACTIVO);
        empresa.setActivo(false);
        em.flush();
        assertThatThrownBy(() -> servicio.invitar(admin.getId(), datos("nuevo@example.com", RolAcceso.LECTURA)))
                .isInstanceOf(AccesoColaboracionException.class);
        assertThat(usuarios.count()).isEqualTo(1);
    }

    @Test
    void rechazaActorInexistenteEIdentificadoresInvalidos() {
        assertThatThrownBy(() -> servicio.invitar(Long.MAX_VALUE, datos("nuevo@example.com", RolAcceso.LECTURA)))
                .isInstanceOf(AccesoColaboracionException.class);
        for (Long id : new Long[]{null, 0L, -1L}) {
            assertThatThrownBy(() -> servicio.invitar(id, datos("nuevo@example.com", RolAcceso.LECTURA)))
                    .isInstanceOf(ConstraintViolationException.class);
        }
        assertThat(usuarios.count()).isEqualTo(1);
    }

    @Test
    void correoExistenteEnOtraEmpresaNoSeTrasladaNiSeReinvita() {
        Empresa otra = empresas.save(Empresa.builder().nombre("Otra").nit(UUID.randomUUID().toString())
                .correoContacto("otra@example.com").activo(true).build());
        Usuario existente = usuarios.save(Usuario.builder().empresa(otra).nombre("Existente")
                .correo("Existente@Example.com").rolAcceso(RolAcceso.LECTURA).estado(EstadoUsuario.INACTIVO).build());
        em.flush();
        Long version = existente.getVersion();

        assertThatThrownBy(() -> servicio.invitar(admin.getId(), datos("existente@example.com", RolAcceso.EDITOR)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(usuarios.count()).isEqualTo(2);
        em.clear();
        Usuario conservado = usuarios.findById(existente.getId()).orElseThrow();
        assertThat(conservado.getEmpresa().getId()).isEqualTo(otra.getId());
        assertThat(conservado.getEstado()).isEqualTo(EstadoUsuario.INACTIVO);
        assertThat(conservado.getVersion()).isEqualTo(version);
        assertThat(conservado.getTokenInvitacionHash()).isNull();
    }

    @Test
    void invitacionRepetidaConservaElTokenYNoDuplicaLaCuenta() {
        var primera = servicio.invitar(admin.getId(), datos("nuevo@example.com", RolAcceso.LECTURA));
        String hash = usuarios.findById(primera.getUsuario().getId()).orElseThrow().getTokenInvitacionHash();
        assertThatThrownBy(() -> servicio.invitar(admin.getId(), datos("NUEVO@example.com", RolAcceso.EDITOR)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(usuarios.count()).isEqualTo(2);
        assertThat(usuarios.findById(primera.getUsuario().getId()).orElseThrow().getTokenInvitacionHash()).isEqualTo(hash);
    }

    @Test
    void validaDatosObligatoriosFormatoYLongitudesAntesDeGuardar() {
        assertThatThrownBy(() -> servicio.invitar(admin.getId(), null))
                .isInstanceOf(ConstraintViolationException.class);
        var vacio = new InvitarUsuarioRequestDto();
        assertThatThrownBy(() -> servicio.invitar(admin.getId(), vacio))
                .isInstanceOf(ConstraintViolationException.class);
        var incorrecto = datos("correo-invalido", RolAcceso.EDITOR);
        incorrecto.setNombre(" ");
        assertThatThrownBy(() -> servicio.invitar(admin.getId(), incorrecto))
                .isInstanceOf(ConstraintViolationException.class);
        var largo = datos("a".repeat(245) + "@example.com", RolAcceso.LECTURA);
        largo.setNombre("a".repeat(256));
        assertThatThrownBy(() -> servicio.invitar(admin.getId(), largo))
                .isInstanceOf(ConstraintViolationException.class);
        assertThat(usuarios.count()).isEqualTo(1);
    }

    @Test
    void cadaInvitacionTieneTokenDistintoYNoPermiteElegirEmpresaEnElDto() {
        var primera = servicio.invitar(admin.getId(), datos("primera@example.com", RolAcceso.LECTURA));
        var segunda = servicio.invitar(admin.getId(), datos("segunda@example.com", RolAcceso.LECTURA));
        assertThat(primera.getTokenInvitacion()).isNotEqualTo(segunda.getTokenInvitacion());
        assertThat(InvitarUsuarioRequestDto.class.getDeclaredFields())
                .extracting(java.lang.reflect.Field::getName)
                .containsExactlyInAnyOrder("nombre", "correo", "rolAcceso");
    }

    private InvitarUsuarioRequestDto datos(String correo, RolAcceso rol) {
        var datos = new InvitarUsuarioRequestDto();
        datos.setNombre("Persona");
        datos.setCorreo(correo);
        datos.setRolAcceso(rol);
        return datos;
    }
}
