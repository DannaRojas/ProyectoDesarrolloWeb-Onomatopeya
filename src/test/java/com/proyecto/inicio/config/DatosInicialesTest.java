package com.proyecto.inicio.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.proyecto.inicio.entity.Usuario;
import com.proyecto.inicio.entity.enums.EstadoPublicacion;
import com.proyecto.inicio.repository.ArcoRepository;
import com.proyecto.inicio.repository.CampoMensajeRepository;
import com.proyecto.inicio.repository.EmpresaRepository;
import com.proyecto.inicio.repository.FlujoMensajeRepository;
import com.proyecto.inicio.repository.LaneRepository;
import com.proyecto.inicio.repository.MensajeRepository;
import com.proyecto.inicio.repository.NodoRepository;
import com.proyecto.inicio.repository.PoolRepository;
import com.proyecto.inicio.repository.ProcesoRepository;
import com.proyecto.inicio.repository.RolProcesoRepository;
import com.proyecto.inicio.repository.UsuarioRepository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.Pbkdf2PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {
            "spring.datasource.url=jdbc:h2:mem:datos-iniciales;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
            "spring.datasource.username=sa",
            "spring.datasource.password=",
            "spring.jpa.hibernate.ddl-auto=create-drop",
            "spring.jpa.properties.hibernate.hbm2ddl.halt_on_error=true",
            "app.datos-demo=true"
        })
@Transactional
class DatosInicialesTest {
    @Autowired DatosIniciales inicializador;
    @Autowired EmpresaRepository empresas;
    @Autowired UsuarioRepository usuarios;
    @Autowired ProcesoRepository procesos;
    @Autowired PoolRepository pools;
    @Autowired RolProcesoRepository roles;
    @Autowired LaneRepository lanes;
    @Autowired NodoRepository nodos;
    @Autowired ArcoRepository arcos;
    @Autowired MensajeRepository mensajes;
    @Autowired CampoMensajeRepository campos;
    @Autowired FlujoMensajeRepository flujos;
    @Autowired com.proyecto.inicio.service.DiagramaService diagramas;

    @Test
    void preparaDosEmpresasConUsuariosYProcesosSeparados() {
        assertThat(empresas.count()).isEqualTo(2);
        assertThat(usuarios.count()).isEqualTo(6);
        assertThat(procesos.count()).isEqualTo(2);
        assertThat(pools.count()).isEqualTo(4);
        assertThat(roles.count()).isEqualTo(2);
        assertThat(lanes.count()).isEqualTo(2);
        assertThat(nodos.count()).isEqualTo(14);
        assertThat(arcos.count()).isEqualTo(14);
        assertThat(mensajes.count()).isEqualTo(4);
        assertThat(campos.count()).isEqualTo(4);
        assertThat(flujos.count()).isEqualTo(4);
        procesos.findAll()
                .forEach(
                        proceso -> {
                            assertThat(proceso.getEstadoPublicacion())
                                    .isEqualTo(EstadoPublicacion.BORRADOR);
                            assertThat(proceso.getCreador().getEmpresa().getId())
                                    .isEqualTo(proceso.getEmpresa().getId());
                            assertThat(
                                            diagramas
                                                    .consultar(
                                                            proceso.getCreador().getId(),
                                                            proceso.getId())
                                                    .advertencias())
                                    .isEmpty();
                        });
        lanes.findAll()
                .forEach(
                        lane ->
                                assertThat(lane.getRolProceso().getEmpresa().getId())
                                        .isEqualTo(
                                                lane.getPool().getProceso().getEmpresa().getId()));
    }

    @Test
    void ejecutarDeNuevoNoDuplicaNiSobrescribeDatos() {
        Usuario editor = usuarios.findByCorreo("editor.uno@example.com").orElseThrow();
        editor.setNombre("Nombre editado en la prueba");
        String hash = editor.getPasswordHash();
        inicializador.run();
        assertThat(empresas.count()).isEqualTo(2);
        assertThat(usuarios.count()).isEqualTo(6);
        assertThat(procesos.count()).isEqualTo(2);
        assertThat(pools.count()).isEqualTo(4);
        assertThat(roles.count()).isEqualTo(2);
        assertThat(lanes.count()).isEqualTo(2);
        assertThat(nodos.count()).isEqualTo(14);
        assertThat(arcos.count()).isEqualTo(14);
        assertThat(mensajes.count()).isEqualTo(4);
        assertThat(editor.getNombre()).isEqualTo("Nombre editado en la prueba");
        assertThat(editor.getPasswordHash()).isEqualTo(hash);
    }

    @Test
    void contrasenasSeGuardanComoHash() {
        Pbkdf2PasswordEncoder encoder = Pbkdf2PasswordEncoder.defaultsForSpringSecurity_v5_8();
        usuarios.findAll()
                .forEach(
                        usuario -> {
                            assertThat(usuario.getPasswordHash()).isNotEqualTo("DemoEntrega1!2026");
                            assertThat(
                                            encoder.matches(
                                                    "DemoEntrega1!2026", usuario.getPasswordHash()))
                                    .isTrue();
                        });
    }
}
