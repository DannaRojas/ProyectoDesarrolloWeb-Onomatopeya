package com.proyecto.inicio.controller;

import static org.assertj.core.api.Assertions.assertThat;

import com.proyecto.inicio.entity.Usuario;
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

@ActiveProfiles("conexion-empresa")
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
            "spring.datasource.url=jdbc:postgresql://127.0.0.1:5433/pruebas_conexion",
            "spring.datasource.username=gestor_pruebas",
            "spring.datasource.password=${DB_PASSWORD}",
            "spring.jpa.hibernate.ddl-auto=create-drop",
            "spring.jpa.properties.hibernate.hbm2ddl.halt_on_error=true"
        })
class EmpresaControllerHttpTest {
    @LocalServerPort
    private int puerto;
    @Autowired
    private EmpresaRepository empresas;
    @Autowired
    private UsuarioRepository usuarios;
    @Autowired
    private PasswordEncoder codificador;
    private final ObjectMapper json = new ObjectMapper();
    private final HttpClient cliente = HttpClient.newHttpClient();

    @Test
    void registraEmpresaYAdminPeroNoPermiteGestionAnonima() throws Exception {
        String nit = UUID.randomUUID().toString().substring(0, 30);
        String correo = nit + "@ejemplo.co";
        HttpResponse<String> respuesta = enviar("POST", "/empresas", registro(nit, correo));
        assertThat(respuesta.statusCode()).isEqualTo(201);
        long id = json.readTree(respuesta.body()).get("id").asLong();
        assertThat(respuesta.headers().firstValue("Location")).hasValue(base() + "/empresas/" + id);
        assertThat(respuesta.body()).doesNotContain("contrasena", "passwordHash");
        Usuario admin = usuarios.findByCorreo(correo).orElseThrow();
        assertThat(admin.getEmpresa().getId()).isEqualTo(id);
        assertThat(admin.getRolAcceso()).isEqualTo(RolAcceso.ADMINISTRADOR);
        assertThat(admin.getEstado()).isEqualTo(EstadoUsuario.ACTIVO);
        assertThat(admin.getPasswordHash()).isNotEqualTo("ClaveDePrueba123!");
        assertThat(codificador.matches("ClaveDePrueba123!", admin.getPasswordHash())).isTrue();
        assertThat(empresas.findById(id).orElseThrow().getActivo()).isTrue();
        assertThat(enviar("GET", "/empresas/" + id, null).statusCode()).isEqualTo(401);
        assertThat(enviar("GET", "/empresas", null).statusCode()).isEqualTo(401);

        HttpResponse<String> cambio =
                enviar(
                        "PUT",
                        "/empresas/" + id,
                        json.writeValueAsString(
                                Map.of(
                                        "nombre",
                                        "Nombre actualizado",
                                        "nit",
                                        nit,
                                        "correoContacto",
                                        "contacto@ejemplo.co")));
        assertThat(cambio.statusCode()).isEqualTo(401);
        assertThat(empresas.findById(id).orElseThrow().getNombre()).isEqualTo("Empresa prueba");
    }

    @Test
    void rechazaNitYCorreoRepetidosSinDejarRegistrosParciales() throws Exception {
        String nit = UUID.randomUUID().toString().substring(0, 30);
        String correo = nit + "@ejemplo.co";
        assertThat(enviar("POST", "/empresas", registro(nit, correo)).statusCode()).isEqualTo(201);
        long totalEmpresas = empresas.count();
        long totalUsuarios = usuarios.count();
        assertThat(enviar("POST", "/empresas", registro(nit, "otro-" + correo)).statusCode())
                .isEqualTo(409);
        String otroNit = UUID.randomUUID().toString().substring(0, 30);
        assertThat(enviar("POST", "/empresas", registro(otroNit, correo)).statusCode())
                .isEqualTo(409);
        assertThat(empresas.findByNit(otroNit)).isEmpty();
        assertThat(empresas.count()).isEqualTo(totalEmpresas);
        assertThat(usuarios.count()).isEqualTo(totalUsuarios);
    }

    @Test
    void validaEmpresaYAdministradorAntesDeGuardar() throws Exception {
        long total = empresas.count();
        HttpResponse<String> respuesta =
                enviar(
                        "POST",
                        "/empresas",
                        """
                        {"empresa":{"nombre":" ","nit":"1","correoContacto":"incorrecto"},
                         "administrador":{"nombre":"","correo":"incorrecto","contrasena":"123"}}
                        """);
        assertThat(respuesta.statusCode()).isEqualTo(400);
        assertThat(json.readTree(respuesta.body()).get("campos").has("administrador.contrasena"))
                .isTrue();
        assertThat(empresas.count()).isEqualTo(total);
        assertThat(enviar("POST", "/empresas", "{}").statusCode()).isEqualTo(400);
        assertThat(enviar("POST", "/empresas", "{").statusCode()).isEqualTo(400);
    }

    @Test
    void traduceIdInvalidoYEmpresaInexistente() throws Exception {
        assertThat(enviar("GET", "/empresas/0", null).statusCode()).isEqualTo(400);
        assertThat(enviar("GET", "/empresas/texto", null).statusCode()).isEqualTo(400);
        assertThat(enviar("GET", "/empresas/999999999", null).statusCode()).isEqualTo(401);
    }

    @Test
    void noExponeEliminacionDeEmpresas() throws Exception {
        assertThat(enviar("DELETE", "/empresas/1", null).statusCode()).isEqualTo(405);
    }

    @Test
    void publicaDocumentacionOpenApi() throws Exception {
        HttpResponse<String> respuesta = enviar("GET", "/v3/api-docs", null);
        assertThat(respuesta.statusCode()).isEqualTo(200);
        assertThat(json.readTree(respuesta.body()).get("paths").has("/empresas")).isTrue();
        assertThat(respuesta.body().split("\"summary\"", -1).length - 1).isEqualTo(59);
    }

    @Test
    void publicaInterfazSwagger() throws Exception {
        HttpResponse<String> respuesta = enviar("GET", "/swagger-ui/index.html", null);
        assertThat(respuesta.statusCode()).isEqualTo(200);
        assertThat(respuesta.body()).contains("Swagger UI");
    }

    @Test
    void correoDuplicadoConMayusculasNoCreaOtraEmpresa() throws Exception {
        String correo = UUID.randomUUID() + "@ejemplo.co";
        assertThat(enviar("POST", "/empresas",
                registro(UUID.randomUUID().toString().substring(0, 30), correo)).statusCode())
                .isEqualTo(201);
        String nit = UUID.randomUUID().toString().substring(0, 30);
        assertThat(enviar("POST", "/empresas",
                registro(nit, correo.toUpperCase(java.util.Locale.ROOT))).statusCode())
                .isEqualTo(409);
        assertThat(empresas.findByNit(nit)).isEmpty();
    }

    private String registro(String nit, String correo) {
        return json.writeValueAsString(
                Map.of(
                        "empresa",
                                Map.of(
                                        "nombre",
                                        "Empresa prueba",
                                        "nit",
                                        nit,
                                        "correoContacto",
                                        "contacto@ejemplo.co"),
                        "administrador",
                                Map.of(
                                        "nombre",
                                        "Admin prueba",
                                        "correo",
                                        correo,
                                        "contrasena",
                                        "ClaveDePrueba123!")));
    }

    private String base() {
        return "http://127.0.0.1:" + puerto;
    }

    private HttpResponse<String> enviar(String metodo, String ruta, String cuerpo)
            throws Exception {
        HttpRequest.BodyPublisher contenido =
                cuerpo == null
                        ? HttpRequest.BodyPublishers.noBody()
                        : HttpRequest.BodyPublishers.ofString(cuerpo);
        return cliente.send(
                HttpRequest.newBuilder(URI.create(base() + ruta))
                        .timeout(Duration.ofSeconds(20))
                        .header("Content-Type", "application/json")
                        .method(metodo, contenido)
                        .build(),
                HttpResponse.BodyHandlers.ofString());
    }
}
