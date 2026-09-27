package com.proyecto.inicio.controller.mensajeria;

import com.proyecto.inicio.entity.*;
import com.proyecto.inicio.entity.enums.*;
import com.proyecto.inicio.repository.MensajeRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;
import tools.jackson.databind.ObjectMapper;
import java.util.Map;
import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ActiveProfiles("conexion-empresa")
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:pruebas-http-mensajes;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa", "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.properties.hibernate.hbm2ddl.halt_on_error=true"
})
@Transactional
@Sql(scripts = "/esquema-nodos-mensajeria.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_CLASS)
class MensajeriaHttpTest {
    @Autowired WebApplicationContext aplicacion;
    @Autowired EntityManager em;
    @Autowired MensajeRepository mensajes;
    MockMvc mvc;
    final ObjectMapper json = new ObjectMapper();
    Empresa empresa;
    Usuario admin, lector;
    Proceso proceso;
    Pool origen, destino;
    Evento envio, recepcion;
    Actividad actividad;

    @BeforeEach
    void preparar() {
        mvc = MockMvcBuilders.webAppContextSetup(aplicacion).build();
        empresa = Empresa.builder().nombre("Empresa HTTP").nit(UUID.randomUUID().toString())
                .correoContacto("contacto@ejemplo.co").activo(true).build();
        em.persist(empresa);
        admin = usuario(RolAcceso.ADMINISTRADOR); lector = usuario(RolAcceso.LECTURA);
        proceso = Proceso.builder().empresa(empresa).creador(admin).nombre("HTTP " + UUID.randomUUID()).activo(true).build();
        em.persist(proceso);
        origen = pool(); destino = pool();
        envio = evento(origen, OperacionEventoMensaje.ENVIO);
        recepcion = evento(destino, OperacionEventoMensaje.RECEPCION);
        RolProceso rol = RolProceso.builder().empresa(empresa).nombre("Analista").activo(true).build();
        em.persist(rol);
        Lane lane = Lane.builder().pool(destino).rolProceso(rol).orden(1).altura(100).activo(true).build();
        em.persist(lane);
        actividad = new Actividad(); nodo(actividad, destino); actividad.setTipo(TipoActividad.TAREA); actividad.setLane(lane);
        em.persist(actividad); em.flush();
    }

    @Test
    void requiereSesionYNoAceptaIdentidadDesdeUnaCabecera() throws Exception {
        mvc.perform(get(ruta()).header("X-Usuario-Id", admin.getId())).andExpect(status().isUnauthorized());
        mvc.perform(get(ruta()).principal(() -> "desconocido@ejemplo.co")).andExpect(status().isUnauthorized());
        assertThat(mensajes.count()).isZero();
    }

    @Test
    void registraConsultaActualizaYRetiraSinBorrarLaFila() throws Exception {
        long id = crearMensaje(envio, "ENVIO");
        em.flush(); em.clear();
        mvc.perform(sesion(get(ruta()))).andExpect(status().isOk()).andExpect(jsonPath("$[0].id").value(id));
        var cambios = datos(envio, "ENVIO"); cambios.put("nombre", "Solicitud editada"); cambios.put("version", 0);
        mvc.perform(sesion(put(ruta() + "/" + id)).contentType("application/json").content(json.writeValueAsString(cambios)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.version").value(1));
        mvc.perform(sesion(delete(ruta() + "/" + id)).param("version", "1").param("confirmar", "true"))
                .andExpect(status().isNoContent());
        em.flush(); em.clear();
        assertThat(mensajes.findById(id).orElseThrow().getActivo()).isFalse();
        mvc.perform(sesion(get(ruta()))).andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void validaCuerpoIdsVersionYConfirmacion() throws Exception {
        mvc.perform(sesion(post(ruta())).contentType("application/json").content("{}")).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.nombre").exists());
        mvc.perform(sesion(post(ruta())).contentType("application/json").content("{")).andExpect(status().isBadRequest());
        mvc.perform(sesion(get("/procesos/-1/mensajes"))).andExpect(status().isBadRequest());
        long id = crearMensaje(envio, "ENVIO");
        mvc.perform(sesion(delete(ruta() + "/" + id)).param("version", "0")).andExpect(status().isBadRequest());
        mvc.perform(sesion(delete(ruta() + "/" + id)).param("version", "99").param("confirmar", "true"))
                .andExpect(status().isConflict());
        assertThat(mensajes.findById(id).orElseThrow().getActivo()).isTrue();
    }

    @Test
    void lectorNoEscribeYEmpresaAjenaNoConsulta() throws Exception {
        mvc.perform(post(ruta()).principal(() -> lector.getCorreo()).contentType("application/json")
                .content(json.writeValueAsString(datos(envio, "ENVIO")))).andExpect(status().isForbidden());
        Empresa ajena = Empresa.builder().nombre("Ajena").nit(UUID.randomUUID().toString())
                .correoContacto("ajena@ejemplo.co").activo(true).build();
        em.persist(ajena); lector.setEmpresa(ajena); em.flush();
        mvc.perform(get(ruta()).principal(() -> lector.getCorreo())).andExpect(status().isForbidden());
        mvc.perform(sesion(get("/procesos/999999/mensajes"))).andExpect(status().isNotFound());
    }

    @Test
    void camposTienenSuDtoYNoSeConsultanDesdeOtroMensaje() throws Exception {
        long id = crearMensaje(envio, "ENVIO");
        long otro = crearMensaje(recepcion, "RECEPCION");
        String campos = ruta() + "/" + id + "/campos";
        var respuesta = mvc.perform(sesion(post(campos)).contentType("application/json")
                .content("{\"nombre\":\"radicado\",\"tipoDato\":\"TEXTO\",\"orden\":0}"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.mensajeId").value(id)).andReturn();
        long campo = json.readTree(respuesta.getResponse().getContentAsString()).get("id").asLong();
        mvc.perform(sesion(get(campos))).andExpect(jsonPath("$.length()").value(1));
        mvc.perform(sesion(put(campos + "/" + campo)).contentType("application/json")
                .content("{\"nombre\":\"numero\",\"tipoDato\":\"TEXTO\",\"orden\":1,\"version\":0}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.nombre").value("numero"));
        mvc.perform(sesion(delete(ruta() + "/" + otro + "/campos/" + campo)).param("version", "1").param("confirmar", "true"))
                .andExpect(status().isNotFound());
        mvc.perform(sesion(delete(campos + "/" + campo)).param("version", "1").param("confirmar", "true"))
                .andExpect(status().isNoContent());
    }

    @Test
    void flujoConectaLosPoolsYExponeAdvertencias() throws Exception {
        crearMensaje(envio, "ENVIO"); crearMensaje(recepcion, "RECEPCION");
        String rutaFlujos = "/procesos/" + proceso.getId() + "/flujos-mensaje";
        var datos = Map.of("poolOrigenId", origen.getId(), "poolDestinoId", destino.getId(),
                "nodoOrigenId", envio.getId(), "nodoDestinoId", recepcion.getId());
        var respuesta = mvc.perform(sesion(post(rutaFlujos)).contentType("application/json").content(json.writeValueAsString(datos)))
                .andExpect(status().isCreated()).andReturn();
        long id = json.readTree(respuesta.getResponse().getContentAsString()).get("id").asLong();
        em.flush(); em.clear();
        mvc.perform(sesion(get(rutaFlujos))).andExpect(jsonPath("$[0].id").value(id));
        mvc.perform(sesion(get(ruta() + "/advertencias"))).andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(0));
        mvc.perform(sesion(delete(rutaFlujos + "/" + id)).param("version", "0").param("confirmar", "true"))
                .andExpect(status().isNoContent());
    }

    @Test
    void actividadConsumeMensajeRecibido() throws Exception {
        long mensaje = crearMensaje(recepcion, "RECEPCION");
        String usos = ruta() + "/" + mensaje + "/usos";
        var respuesta = mvc.perform(sesion(post(usos)).contentType("application/json")
                .content(json.writeValueAsString(Map.of("actividadId", actividad.getId()))))
                .andExpect(status().isCreated()).andReturn();
        long id = json.readTree(respuesta.getResponse().getContentAsString()).get("id").asLong();
        mvc.perform(sesion(get(usos))).andExpect(jsonPath("$[0].actividadId").value(actividad.getId()));
        mvc.perform(sesion(delete(usos + "/" + id)).param("version", "0").param("confirmar", "true"))
                .andExpect(status().isNoContent());
    }

    @Test
    void soloAdministradorConfiguraPermisos() throws Exception {
        String permisos = "/procesos/" + proceso.getId() + "/permisos-estructura";
        String cuerpo = "{\"rolAcceso\":\"EDITOR\",\"recurso\":\"POOL\",\"accion\":\"CREAR\",\"permitido\":true}";
        mvc.perform(sesion(put(permisos)).contentType("application/json").content(cuerpo))
                .andExpect(status().isOk()).andExpect(jsonPath("$.permitido").value(true));
        mvc.perform(sesion(get(permisos))).andExpect(jsonPath("$.length()").value(1));
        mvc.perform(get(permisos).principal(() -> lector.getCorreo())).andExpect(status().isForbidden());
        mvc.perform(get(permisos + "/verificar").principal(() -> lector.getCorreo()).param("recurso", "POOL").param("accion", "CREAR"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.permitido").value(false));
    }

    private String ruta() { return "/procesos/" + proceso.getId() + "/mensajes"; }
    private MockHttpServletRequestBuilder sesion(MockHttpServletRequestBuilder peticion) {
        // Solo la prueba aporta un Principal; el servidor real deberá obtenerlo del login.
        return peticion.principal(() -> admin.getCorreo());
    }
    private java.util.HashMap<String, Object> datos(Nodo nodo, String sentido) {
        return new java.util.HashMap<>(Map.of("nodoId", nodo.getId(), "sentido", sentido, "nombre", "Solicitud",
                "origenExterno", false, "correlacionTipo", "NEGOCIO", "correlacionNegocio", "radicado"));
    }
    private long crearMensaje(Nodo nodo, String sentido) throws Exception {
        var respuesta = mvc.perform(sesion(post(ruta())).contentType("application/json")
                .content(json.writeValueAsString(datos(nodo, sentido)))).andExpect(status().isCreated()).andReturn();
        return json.readTree(respuesta.getResponse().getContentAsString()).get("id").asLong();
    }
    private Usuario usuario(RolAcceso rol) {
        Usuario usuario = Usuario.builder().empresa(empresa).nombre("Usuario").correo(UUID.randomUUID() + "@ejemplo.co")
                .rolAcceso(rol).estado(EstadoUsuario.ACTIVO).build();
        em.persist(usuario); return usuario;
    }
    private Pool pool() {
        Pool pool = Pool.builder().proceso(proceso).nombre("Participante").tipoParticipante(TipoParticipante.EMPRESA)
                .propietario(false).cajaNegra(false).posicionX(0).posicionY(0).ancho(800).alto(400).orden(1).activo(true).build();
        em.persist(pool); return pool;
    }
    private Evento evento(Pool pool, OperacionEventoMensaje operacion) {
        Evento evento = new Evento(); nodo(evento, pool);
        evento.setTipo(TipoEvento.INTERMEDIO); evento.setNaturaleza(NaturalezaEvento.MENSAJE); evento.setOperacionMensaje(operacion);
        em.persist(evento); return evento;
    }
    private void nodo(Nodo nodo, Pool pool) {
        nodo.setProceso(proceso); nodo.setPool(pool); nodo.setNombre("Nodo");
        nodo.setPosicionX(10); nodo.setPosicionY(10); nodo.setAncho(100); nodo.setAlto(60);
    }
}
