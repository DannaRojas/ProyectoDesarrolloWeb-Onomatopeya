package com.proyecto.inicio.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import com.proyecto.inicio.entity.Usuario;
import com.proyecto.inicio.entity.enums.EstadoUsuario;
import com.proyecto.inicio.entity.enums.RolAcceso;
import com.proyecto.inicio.repository.EmpresaRepository;
import com.proyecto.inicio.repository.UsuarioRepository;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import tools.jackson.databind.ObjectMapper;

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

    @Autowired
    private AuthenticationManager autenticador;

    @Autowired
    private UserDetailsService detalles;

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
        assertThat(detalles.loadUserByUsername(correo).getUsername()).isEqualTo(correo);
        assertThatCode(() -> autenticador.authenticate(
                new UsernamePasswordAuthenticationToken(correo, "ClaveDePrueba123!")))
                .doesNotThrowAnyException();
        assertThat(empresas.findById(id).orElseThrow().getActivo()).isTrue();
        assertThat(enviar("GET", "/empresas/" + id, null).statusCode()).isEqualTo(401);
        assertThat(enviar("GET", "/empresas", null).statusCode()).isEqualTo(401);
        assertThat(enviarAutorizado("GET", "/empresas/" + id, null, token(correo))
                .statusCode()).isEqualTo(200);

        Map<String, String> datosCambio = Map.of(
                "nombre", "Nombre actualizado",
                "nit", nit,
                "correoContacto", "contacto@ejemplo.co");
        HttpResponse<String> cambio = enviar(
                "PUT", "/empresas/" + id, json.writeValueAsString(datosCambio));
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
        HttpResponse<String> respuesta = enviar(
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
        String nit = UUID.randomUUID().toString().substring(0, 30);
        String correo = nit + "@ejemplo.co";
        assertThat(enviar("POST", "/empresas", registro(nit, correo)).statusCode()).isEqualTo(201);
        String token = token(correo);
        assertThat(enviarAutorizado("GET", "/empresas/0", null, token).statusCode()).isEqualTo(400);
        assertThat(enviarAutorizado("GET", "/empresas/texto", null, token).statusCode()).isEqualTo(400);
        assertThat(enviarAutorizado("GET", "/empresas/999999999", null, token).statusCode()).isEqualTo(403);
    }

    @Test
    void noExponeEliminacionDeEmpresas() throws Exception {
        String nit = UUID.randomUUID().toString().substring(0, 30);
        String correo = nit + "@ejemplo.co";
        assertThat(enviar("POST", "/empresas", registro(nit, correo)).statusCode()).isEqualTo(201);
        assertThat(enviarAutorizado("DELETE", "/empresas/1", null, token(correo))
                .statusCode()).isEqualTo(405);
    }

    @Test
    void publicaDocumentacionOpenApi() throws Exception {
        HttpResponse<String> respuesta = enviar("GET", "/v3/api-docs", null);
        assertThat(respuesta.statusCode()).isEqualTo(200);
        assertThat(json.readTree(respuesta.body()).get("paths").has("/empresas")).isTrue();
        assertThat(respuesta.body().split("\"summary\"", -1).length - 1).isEqualTo(60);
        assertThat(json.readTree(respuesta.body()).get("components").get("securitySchemes")
                .has("bearerAuth")).isTrue();
        assertThat(json.readTree(respuesta.body()).get("paths").get("/empresas").get("get")
                .has("security")).isTrue();
        assertThat(json.readTree(respuesta.body()).get("paths").get("/empresas").get("post")
                .has("security")).isFalse();
        assertThat(json.readTree(respuesta.body()).get("paths").get("/auth/login").get("post")
                .get("description").asText()).contains("token Bearer");
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

    @Test
    void loginRechazaClaveIncorrectaYUsuarioDesactivado() throws Exception {
        String nit = UUID.randomUUID().toString().substring(0, 30);
        String correo = nit + "@ejemplo.co";
        assertThat(enviar("POST", "/empresas", registro(nit, correo)).statusCode()).isEqualTo(201);
        assertThat(enviar("POST", "/auth/login", json.writeValueAsString(
                Map.of("correo", correo, "contrasena", "incorrecta"))).statusCode())
                .isEqualTo(401);
        String token = token(correo);
        assertThat(enviarAutorizado("GET", "/empresas", null, token).statusCode()).isEqualTo(200);
        Usuario usuario = usuarios.findByCorreo(correo).orElseThrow();
        usuario.setEstado(EstadoUsuario.INACTIVO);
        usuarios.saveAndFlush(usuario);
        assertThat(enviarAutorizado("GET", "/empresas", null, token).statusCode()).isEqualTo(401);
    }

    @Test
    void tokenNoPermiteConsultarUnaEmpresaAjena() throws Exception {
        String nitPropio = UUID.randomUUID().toString().substring(0, 30);
        String correoPropio = nitPropio + "@ejemplo.co";
        assertThat(enviar("POST", "/empresas", registro(nitPropio, correoPropio)).statusCode())
                .isEqualTo(201);
        String nitAjeno = UUID.randomUUID().toString().substring(0, 30);
        HttpResponse<String> ajena = enviar("POST", "/empresas",
                registro(nitAjeno, nitAjeno + "@ejemplo.co"));
        long idAjeno = json.readTree(ajena.body()).get("id").asLong();
        assertThat(enviarAutorizado("GET", "/empresas/" + idAjeno, null,
                token(correoPropio)).statusCode()).isEqualTo(403);
        assertThat(enviarAutorizado("GET", "/empresas", null, "token-inventado")
                .statusCode()).isEqualTo(401);
    }

    @Test
    void laSesionPermiteListarSoloLosProcesosDeSuEmpresa() throws Exception {
        String nit = UUID.randomUUID().toString().substring(0, 30);
        String correo = nit + "@ejemplo.co";
        assertThat(enviar("POST", "/empresas", registro(nit, correo)).statusCode()).isEqualTo(201);
        assertThat(enviar("GET", "/procesos", null).statusCode()).isEqualTo(401);

        String sesion = token(correo);
        HttpResponse<String> proceso = enviarAutorizado("POST", "/procesos",
                json.writeValueAsString(Map.of("nombre", "Compras " + nit,
                        "estadoPublicacion", "BORRADOR")), sesion);
        assertThat(proceso.statusCode()).withFailMessage(proceso.body()).isEqualTo(201);

        String nitAjeno = UUID.randomUUID().toString().substring(0, 30);
        String correoAjeno = nitAjeno + "@ejemplo.co";
        assertThat(enviar("POST", "/empresas", registro(nitAjeno, correoAjeno)).statusCode())
                .isEqualTo(201);
        String sesionAjena = token(correoAjeno);
        HttpResponse<String> procesoAjeno = enviarAutorizado("POST", "/procesos",
                json.writeValueAsString(Map.of("nombre", "Ventas " + nitAjeno,
                        "estadoPublicacion", "BORRADOR")), sesionAjena);
        assertThat(procesoAjeno.statusCode()).withFailMessage(procesoAjeno.body()).isEqualTo(201);

        HttpResponse<String> listado = enviarAutorizado("GET", "/procesos", null, sesion);
        assertThat(listado.statusCode()).withFailMessage(listado.body()).isEqualTo(200);
        assertThat(listado.body()).contains("Compras " + nit).doesNotContain("Ventas " + nitAjeno);

        HttpResponse<String> listadoAjeno = enviarAutorizado("GET", "/procesos", null, sesionAjena);
        assertThat(listadoAjeno.statusCode()).withFailMessage(listadoAjeno.body()).isEqualTo(200);
        assertThat(listadoAjeno.body()).contains("Ventas " + nitAjeno).doesNotContain("Compras " + nit);
    }

    private String token(String correo) throws Exception {
        HttpResponse<String> respuesta = enviar("POST", "/auth/login", json.writeValueAsString(
                Map.of("correo", correo, "contrasena", "ClaveDePrueba123!")));
        assertThat(respuesta.statusCode())
                .withFailMessage("Login respondió %s: %s", respuesta.statusCode(), respuesta.body())
                .isEqualTo(200);
        return json.readTree(respuesta.body()).get("token").asText();
    }

    private String registro(String nit, String correo) {
        Map<String, String> empresa = Map.of(
                "nombre", "Empresa prueba",
                "nit", nit,
                "correoContacto", "contacto@ejemplo.co");
        Map<String, String> administrador = Map.of(
                "nombre", "Admin prueba",
                "correo", correo,
                "contrasena", "ClaveDePrueba123!");
        return json.writeValueAsString(Map.of(
                "empresa", empresa,
                "administrador", administrador));
    }

    private String base() {
        return "http://127.0.0.1:" + puerto;
    }

    private HttpResponse<String> enviar(String metodo, String ruta, String cuerpo)
            throws Exception {
        return enviarAutorizado(metodo, ruta, cuerpo, null);
    }

    private HttpResponse<String> enviarAutorizado(String metodo, String ruta, String cuerpo,
            String token) throws Exception {
        HttpRequest.BodyPublisher contenido = cuerpo == null
                ? HttpRequest.BodyPublishers.noBody()
                : HttpRequest.BodyPublishers.ofString(cuerpo);
        HttpRequest.Builder peticion = HttpRequest.newBuilder(URI.create(base() + ruta))
                .timeout(Duration.ofSeconds(20))
                .header("Content-Type", "application/json")
                .method(metodo, contenido);
        if (token != null) {
            peticion.header("Authorization", "Bearer " + token);
        }
        return cliente.send(peticion.build(), HttpResponse.BodyHandlers.ofString());
    }
}
