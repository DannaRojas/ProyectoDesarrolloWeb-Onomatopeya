package com.proyecto.inicio.integracion.empresa;

import com.proyecto.inicio.entity.enums.EstadoUsuario;
import com.proyecto.inicio.entity.enums.RolAcceso;
import com.proyecto.inicio.repository.EmpresaRepository;
import com.proyecto.inicio.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import tools.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("conexion-empresa")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "spring.datasource.url=jdbc:h2:mem:pruebas-conexion;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa", "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.properties.hibernate.hbm2ddl.halt_on_error=true"
})
class ConexionEmpresaHttpTest {
    @LocalServerPort private int puerto;
    @Autowired private EmpresaRepository empresas;
    @Autowired private UsuarioRepository usuarios;
    @Autowired private PasswordEncoder codificador;
    private final ObjectMapper json = new ObjectMapper();
    private final HttpClient cliente = HttpClient.newHttpClient();

    @Test
    void registraEmpresaYAdminLuegoPermiteConsultarYActualizar() throws Exception {
        String nit = UUID.randomUUID().toString();
        String correo = nit + "@ejemplo.co";
        var respuesta = enviar("POST", "/empresas", registro(nit, correo));
        assertThat(respuesta.statusCode()).isEqualTo(201);
        long id = json.readTree(respuesta.body()).get("id").asLong();
        assertThat(respuesta.headers().firstValue("Location")).hasValue(base() + "/empresas/" + id);
        assertThat(respuesta.body()).doesNotContain("contrasena", "passwordHash");
        var admin = usuarios.findByCorreo(correo).orElseThrow();
        assertThat(admin.getEmpresa().getId()).isEqualTo(id);
        assertThat(admin.getRolAcceso()).isEqualTo(RolAcceso.ADMINISTRADOR);
        assertThat(admin.getEstado()).isEqualTo(EstadoUsuario.ACTIVO);
        assertThat(admin.getPasswordHash()).isNotEqualTo("ClaveDePrueba123!");
        assertThat(codificador.matches("ClaveDePrueba123!", admin.getPasswordHash())).isTrue();
        assertThat(empresas.findById(id).orElseThrow().getActivo()).isTrue();
        assertThat(enviar("GET", "/empresas/" + id, null).statusCode()).isEqualTo(200);
        assertThat(enviar("GET", "/empresas", null).statusCode()).isEqualTo(200);

        var cambio = enviar("PUT", "/empresas/" + id, json.writeValueAsString(Map.of(
                "nombre", "Nombre actualizado", "nit", nit, "correoContacto", "contacto@ejemplo.co")));
        assertThat(cambio.statusCode()).isEqualTo(200);
        assertThat(empresas.findById(id).orElseThrow().getNombre()).isEqualTo("Nombre actualizado");
    }

    @Test
    void rechazaNitYCorreoRepetidosSinDejarRegistrosParciales() throws Exception {
        String nit = UUID.randomUUID().toString();
        String correo = nit + "@ejemplo.co";
        assertThat(enviar("POST", "/empresas", registro(nit, correo)).statusCode()).isEqualTo(201);
        long totalEmpresas = empresas.count();
        long totalUsuarios = usuarios.count();
        assertThat(enviar("POST", "/empresas", registro(nit, "otro-" + correo)).statusCode()).isEqualTo(409);
        String otroNit = UUID.randomUUID().toString();
        assertThat(enviar("POST", "/empresas", registro(otroNit, correo)).statusCode()).isEqualTo(409);
        assertThat(empresas.findByNit(otroNit)).isEmpty();
        assertThat(empresas.count()).isEqualTo(totalEmpresas);
        assertThat(usuarios.count()).isEqualTo(totalUsuarios);
    }

    @Test
    void validaEmpresaYAdministradorAntesDeGuardar() throws Exception {
        long total = empresas.count();
        var respuesta = enviar("POST", "/empresas", """
                {"empresa":{"nombre":" ","nit":"1","correoContacto":"incorrecto"},
                 "administrador":{"nombre":"","correo":"incorrecto","contrasena":"123"}}
                """);
        assertThat(respuesta.statusCode()).isEqualTo(400);
        assertThat(json.readTree(respuesta.body()).get("campos").has("administrador.contrasena")).isTrue();
        assertThat(empresas.count()).isEqualTo(total);
        assertThat(enviar("POST", "/empresas", "{}").statusCode()).isEqualTo(400);
        assertThat(enviar("POST", "/empresas", "{").statusCode()).isEqualTo(400);
    }

    @Test
    void traduceIdInvalidoYEmpresaInexistente() throws Exception {
        assertThat(enviar("GET", "/empresas/0", null).statusCode()).isEqualTo(400);
        assertThat(enviar("GET", "/empresas/texto", null).statusCode()).isEqualTo(400);
        assertThat(enviar("GET", "/empresas/999999999", null).statusCode()).isEqualTo(404);
    }

    @Test
    void noExponeEliminacionDeEmpresas() throws Exception {
        assertThat(enviar("DELETE", "/empresas/1", null).statusCode()).isEqualTo(405);
    }

    private String registro(String nit, String correo) {
        return json.writeValueAsString(Map.of(
                "empresa", Map.of("nombre", "Empresa prueba", "nit", nit, "correoContacto", "contacto@ejemplo.co"),
                "administrador", Map.of("nombre", "Admin prueba", "correo", correo, "contrasena", "ClaveDePrueba123!")));
    }

    private String base() { return "http://127.0.0.1:" + puerto; }

    private HttpResponse<String> enviar(String metodo, String ruta, String cuerpo) throws Exception {
        var contenido = cuerpo == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(cuerpo);
        return cliente.send(HttpRequest.newBuilder(URI.create(base() + ruta))
                .timeout(Duration.ofSeconds(20)).header("Content-Type", "application/json")
                .method(metodo, contenido).build(), HttpResponse.BodyHandlers.ofString());
    }
}
