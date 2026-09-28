package com.proyecto.inicio.service;

import com.proyecto.inicio.dto.DiagramaDto.*;
import com.proyecto.inicio.dto.ProcesoDto.*;
import com.proyecto.inicio.dto.PermisoEstructuraDto.PermisoEstructuraRequestDto;
import com.proyecto.inicio.entity.*;
import com.proyecto.inicio.entity.enums.*;
import com.proyecto.inicio.exception.AccesoColaboracionException;
import com.proyecto.inicio.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;
import tools.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ActiveProfiles("conexion-empresa")
@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:entrega-backend;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
    "spring.datasource.username=sa", "spring.datasource.password=",
    "spring.jpa.hibernate.ddl-auto=create-drop", "app.datos-demo=true",
    "spring.jpa.properties.hibernate.hbm2ddl.halt_on_error=true"
})
@Transactional
class EntregaBackendTest {
    @Autowired ProcesoService procesos;
    @Autowired DiagramaService diagramas;
    @Autowired NodoService nodos;
    @Autowired ArcoService arcos;
    @Autowired LaneService lanes;
    @Autowired PoolService pools;
    @Autowired RolProcesoService roles;
    @Autowired PermisoEstructuraService permisos;
    @Autowired UsuarioRepository usuarios;
    @Autowired ProcesoRepository procesoRepo;
    @Autowired NodoRepository nodoRepo;
    @Autowired ArcoRepository arcoRepo;
    @Autowired LaneRepository laneRepo;
    @Autowired RolProcesoRepository rolRepo;
    @Autowired FlujoMensajeRepository flujoRepo;
    @Autowired EmpresaService empresas;
    @Autowired WebApplicationContext aplicacion;
    Usuario admin, editor, lector, ajeno;
    Proceso proceso;
    Detalle detalle;
    MockMvc mvc;
    final ObjectMapper json = new ObjectMapper();

    @BeforeEach
    void preparar() {
        admin = usuarios.findByCorreo("admin.uno@example.com").orElseThrow();
        editor = usuarios.findByCorreo("editor.uno@example.com").orElseThrow();
        lector = usuarios.findByCorreo("lector.uno@example.com").orElseThrow();
        ajeno = usuarios.findByCorreo("admin.dos@example.com").orElseThrow();
        proceso = procesoRepo.findByEmpresaIdAndActivoTrue(admin.getEmpresa().getId()).get(0);
        detalle = diagramas.consultar(admin.getId(), proceso.getId());
        mvc = MockMvcBuilders.webAppContextSetup(aplicacion).build();
    }

    @Test
    void buscaPorNombreCategoriaEstadoYPagina() {
        procesos.actualizar(admin.getId(), proceso.getId(), new Datos("Compra de insumos", "Detalle", "Compras",
                EstadoPublicacion.BORRADOR, proceso.getVersion()));
        var pagina = procesos.buscar(admin.getId(), "INSUMOS", "compras", EstadoPublicacion.BORRADOR, true, 0, 1);
        assertThat(pagina.total()).isEqualTo(1);
        assertThat(pagina.contenido()).extracting(Ficha::id).containsExactly(proceso.getId());
        assertThat(procesos.buscar(admin.getId(), null, "Ventas", null, true, 0, 10).contenido()).isEmpty();
        assertThat(procesos.historial(admin.getId(), proceso.getId())).anyMatch(h -> h.accion() == AccionHistorial.ACTUALIZAR);
    }

    @Test
    void retirarProcesoConservaDatosYPermiteConsultarHistorial() {
        long totalNodos = nodoRepo.count();
        procesos.retirar(admin.getId(), proceso.getId(), new Retirada(proceso.getVersion(), true));
        assertThat(procesos.consultar(admin.getId(), proceso.getId()).activo()).isFalse();
        assertThat(procesos.buscar(admin.getId(), null, null, null, false, 0, 10).total()).isEqualTo(1);
        assertThat(diagramas.consultar(admin.getId(), proceso.getId()).nodos()).hasSize(7);
        assertThat(nodoRepo.count()).isEqualTo(totalNodos);
        assertThat(procesos.historial(admin.getId(), proceso.getId())).anyMatch(h -> h.accion() == AccionHistorial.DESACTIVAR);
    }

    @Test
    void editorNoRetiraProcesoYLectorNoLoEdita() {
        assertThatThrownBy(() -> procesos.retirar(editor.getId(), proceso.getId(), new Retirada(proceso.getVersion(), true)))
                .isInstanceOf(AccesoColaboracionException.class);
        assertThatThrownBy(() -> procesos.actualizar(lector.getId(), proceso.getId(), datosProceso(EstadoPublicacion.BORRADOR)))
                .isInstanceOf(AccesoColaboracionException.class);
    }

    @Test
    void compartirDaSoloLecturaYRevocarRetiraElAcceso() {
        procesos.compartir(admin.getId(), proceso.getId(), new Compartir(ajeno.getEmpresa().getId(), true, proceso.getVersion()));
        assertThat(diagramas.consultar(ajeno.getId(), proceso.getId()).nodos()).hasSize(7);
        assertThat(procesos.buscar(ajeno.getId(), null, null, null, true, 0, 10).total()).isEqualTo(2);
        assertThatThrownBy(() -> procesos.actualizar(ajeno.getId(), proceso.getId(), datosProceso(EstadoPublicacion.BORRADOR)))
                .isInstanceOf(AccesoColaboracionException.class);
    }

    @Test
    void revocarImpideConsultarElDiagramaCompartido() {
        procesos.compartir(admin.getId(), proceso.getId(), new Compartir(ajeno.getEmpresa().getId(), true, proceso.getVersion()));
        procesos.compartir(admin.getId(), proceso.getId(), new Compartir(ajeno.getEmpresa().getId(), false, proceso.getVersion()));
        assertThatThrownBy(() -> diagramas.consultar(ajeno.getId(), proceso.getId())).isInstanceOf(AccesoColaboracionException.class);
    }

    @Test
    void publicaDiagramaCompleto() {
        assertThat(procesos.actualizar(admin.getId(), proceso.getId(), datosProceso(EstadoPublicacion.PUBLICADO))
                .estadoPublicacion()).isEqualTo(EstadoPublicacion.PUBLICADO);
    }

    @Test
    void borradorSinNodosNoSePuedePublicar() {
        Ficha nuevo = procesos.crear(admin.getId(), new Datos("Proceso vacío", null, "Demo", EstadoPublicacion.BORRADOR, null));
        assertThatThrownBy(() -> procesos.actualizar(admin.getId(), nuevo.id(), new Datos(nuevo.nombre(), null, "Demo",
                EstadoPublicacion.PUBLICADO, nuevo.version()))).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void retirarActividadNoBorraArcosFisicamente() {
        NodoVista actividad = tarea();
        long total = arcoRepo.count();
        var conectados = arcoRepo.conectados(actividad.id());
        assertThat(conectados).hasSize(2);
        assertThat(nodos.retirar(admin.getId(), proceso.getId(), actividad.id(), new Retirada(proceso.getVersion(), true)))
                .isNotEmpty();
        assertThat(arcoRepo.count()).isEqualTo(total);
        assertThat(conectados).allMatch(a -> !a.getActivo());
        assertThat(nodoRepo.findById(actividad.id()).orElseThrow().getEstado()).isEqualTo(EstadoNodo.RETIRADO);
        assertThat(procesos.historial(admin.getId(), proceso.getId())).anyMatch(h -> h.tipoObjeto().equals("NODO")
                && h.accion() == AccionHistorial.DESACTIVAR);
    }

    @Test
    void actividadDeManejoDeErrorNoSeRetiraSinReasignar() {
        NodoVista actividad = tarea();
        var flujo = flujoRepo.listarActivos(proceso.getId()).get(0);
        flujo.setActividadError((Actividad) org.hibernate.Hibernate.unproxy(nodoRepo.findById(actividad.id()).orElseThrow()));
        flujo.setPoliticaFallo(PoliticaFallo.DERIVAR);
        flujoRepo.saveAndFlush(flujo);
        assertThatThrownBy(() -> nodos.retirar(admin.getId(), proceso.getId(), actividad.id(), new Retirada(proceso.getVersion(), true)))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("Reasigna");
        assertThat(flujo.getPoliticaFallo()).isEqualTo(PoliticaFallo.DERIVAR);
    }

    @Test
    void editarActividadConservaSusConexiones() {
        NodoVista actividad = tarea();
        assertThat(nodos.actualizar(editor.getId(), proceso.getId(), actividad.id(), datosNodo(actividad, "Aprobar compra"))
                .nombre()).isEqualTo("Aprobar compra");
        assertThat(arcoRepo.conectados(actividad.id())).hasSize(2);
    }

    @Test
    void cambiarGatewayAParaleloLimpiaCondiciones() {
        NodoVista gateway = detalle.nodos().stream().filter(n -> n.clase() == ClaseNodo.GATEWAY).findFirst().orElseThrow();
        var datos = new NodoDatos(ClaseNodo.GATEWAY, gateway.poolId(), gateway.nombre(), 500, 100, 50, 50,
                null, null, TipoGateway.PARALELO, DireccionGateway.DIVERGENCIA, null, null, null);
        nodos.actualizar(editor.getId(), proceso.getId(), gateway.id(), datos);
        assertThat(arcoRepo.salientes(gateway.id())).hasSize(2).allMatch(a -> a.getCondicionSalida() == null);
    }

    @Test
    void gatewayExclusivoRequiereCondicionesDeSalida() {
        NodoVista gateway = detalle.nodos().stream().filter(n -> n.clase() == ClaseNodo.GATEWAY).findFirst().orElseThrow();
        arcoRepo.salientes(gateway.id()).forEach(a -> a.setCondicionSalida(null));
        assertThatThrownBy(() -> nodos.actualizar(editor.getId(), proceso.getId(), gateway.id(), datosNodo(gateway, gateway.nombre())))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("condiciones");
    }

    @Test
    void editarYRetirarArcoMantieneElRegistro() {
        ArcoVista arco = detalle.arcos().get(0);
        assertThat(arcos.actualizar(editor.getId(), proceso.getId(), arco.id(),
                new ArcoDatos(arco.origenId(), arco.destinoId(), "Solicitud", arco.condicionSalida())).etiqueta()).isEqualTo("Solicitud");
        long total = arcoRepo.count();
        arcos.retirar(admin.getId(), proceso.getId(), arco.id(), new Retirada(proceso.getVersion(), true));
        assertThat(arcoRepo.count()).isEqualTo(total);
        assertThat(arcoRepo.findById(arco.id()).orElseThrow().getActivo()).isFalse();
    }

    @Test
    void laneConActividadesNoSeRetira() {
        LaneVista lane = detalle.lanes().get(0);
        assertThatThrownBy(() -> lanes.retirar(admin.getId(), proceso.getId(), lane.poolId(), lane.id(), new Retirada(lane.version(), true)))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("actividades");
    }

    @Test
    void reordenarLanesNoChocaConSuOrdenUnico() {
        LaneVista primera = detalle.lanes().get(0);
        LaneVista segunda = lanes.crear(admin.getId(), proceso.getId(), primera.poolId(), new LaneDatos(primera.rolProcesoId(), 2, 200, null));
        var orden = lanes.reordenar(admin.getId(), proceso.getId(), primera.poolId(),
                new Reordenar(List.of(new OrdenLane(segunda.id(), segunda.version()), new OrdenLane(primera.id(), primera.version()))));
        assertThat(orden).extracting(LaneVista::id).containsExactly(segunda.id(), primera.id());
        assertThat(orden).extracting(LaneVista::orden).doesNotHaveDuplicates();
    }

    @Test
    void cajaNegraNoPermiteNodosNiLanes() {
        PoolVista caja = detalle.pools().stream().filter(PoolVista::cajaNegra).findFirst().orElseThrow();
        assertThatThrownBy(() -> lanes.crear(admin.getId(), proceso.getId(), caja.id(),
                new LaneDatos(detalle.lanes().get(0).rolProcesoId(), 1, 200, null))).isInstanceOf(IllegalArgumentException.class);
        var datos = new NodoDatos(ClaseNodo.EVENTO, caja.id(), "Inicio", 0, 0, 50, 50,
                null, null, null, null, TipoEvento.INICIO, NaturalezaEvento.NORMAL, null);
        assertThatThrownBy(() -> nodos.crear(admin.getId(), proceso.getId(), datos)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void editorNecesitaPermisoExplicitoParaCrearPools() {
        assertThatThrownBy(() -> pools.crear(editor.getId(), proceso.getId(), datosPool())).isInstanceOf(AccesoColaboracionException.class);
    }

    @Test
    void permisoDeEstructuraHabilitaAlEditor() {
        permisos.guardar(admin.getId(), proceso.getId(), new PermisoEstructuraRequestDto(RolAcceso.EDITOR,
                RecursoEstructura.POOL, AccionEstructura.CREAR, true, null));
        assertThat(pools.crear(editor.getId(), proceso.getId(), datosPool()).cajaNegra()).isTrue();
    }

    @Test
    void rolEnUsoNoSeRetiraYElRenombreSeVeEnLaLane() {
        LaneVista lane = detalle.lanes().get(0);
        var rol = rolRepo.findById(lane.rolProcesoId()).orElseThrow();
        roles.actualizar(admin.getId(), rol.getId(), new RolDatos("Gestión de compras", "Responsable", rol.getVersion()));
        assertThat(lanes.listar(admin.getId(), proceso.getId(), lane.poolId()).get(0).nombre()).isEqualTo("Gestión de compras");
        assertThatThrownBy(() -> roles.retirar(admin.getId(), rol.getId(), new Retirada(rol.getVersion(), true)))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void rolSinUsoSeRetiraSinBorrarlo() {
        var rol = roles.crear(admin.getId(), new RolDatos("Rol temporal", null, null));
        roles.retirar(admin.getId(), rol.id(), new Retirada(rol.version(), true));
        assertThat(rolRepo.findById(rol.id()).orElseThrow().getActivo()).isFalse();
    }

    @Test
    void reordenarPoolsConservaElPropietarioYLasVersiones() {
        PoolVista primero = detalle.pools().get(0);
        PoolVista segundo = detalle.pools().get(1);
        var resultado = pools.reordenar(admin.getId(), proceso.getId(), new ReordenarPools(List.of(
                new OrdenPool(segundo.id(), segundo.version()), new OrdenPool(primero.id(), primero.version()))));
        assertThat(resultado).extracting(PoolVista::id).containsExactly(segundo.id(), primero.id());
        assertThat(resultado).filteredOn(PoolVista::propietario).hasSize(1);
        assertThat(resultado).extracting(PoolVista::orden).containsExactly(1, 2);
    }

    @Test
    void empresaUsaJpqlParaConsultarActivas() {
        assertThat(empresas.consultarTodas()).hasSize(2);
        ajeno.getEmpresa().setActivo(false);
        assertThat(empresas.consultarTodas()).hasSize(1);
    }

    @Test
    void gestionDeEstructuraYNodosFuncionaPorHttp() throws Exception {
        String raiz = "/procesos/" + proceso.getId();
        mvc.perform(post("/roles-proceso").principal(admin::getCorreo).contentType("application/json")
                .content(json.writeValueAsString(new RolDatos("Rol HTTP", null, null)))).andExpect(status().isCreated());
        var rol = roles.buscar(admin.getId(), "Rol HTTP", 0, 10).contenido().get(0);
        mvc.perform(put("/roles-proceso/" + rol.id()).principal(admin::getCorreo).contentType("application/json")
                .content(json.writeValueAsString(new RolDatos("Rol actualizado HTTP", null, rol.version()))))
                .andExpect(status().isOk());
        mvc.perform(post(raiz + "/pools").principal(admin::getCorreo).contentType("application/json")
                .content(json.writeValueAsString(datosPool()))).andExpect(status().isCreated());
        var pool = pools.listar(admin.getId(), proceso.getId()).stream()
                .filter(p -> p.nombre().equals("Otro participante")).findFirst().orElseThrow();
        mvc.perform(put(raiz + "/pools/" + pool.id()).principal(admin::getCorreo).contentType("application/json")
                .content(json.writeValueAsString(new PoolDatos("Pool HTTP", pool.tipoParticipante(), true,
                        0, 800, 300, 150, 3, pool.version())))).andExpect(status().isOk());
        Long propietario = detalle.lanes().get(0).poolId();
        String rutaLanes = raiz + "/pools/" + propietario + "/lanes";
        mvc.perform(post(rutaLanes).principal(admin::getCorreo).contentType("application/json")
                .content(json.writeValueAsString(new LaneDatos(rol.id(), 2, 200, null)))).andExpect(status().isCreated());
        LaneVista lane = lanes.listar(admin.getId(), proceso.getId(), propietario).get(1);
        mvc.perform(put(rutaLanes + "/" + lane.id()).principal(admin::getCorreo).contentType("application/json")
                .content(json.writeValueAsString(new LaneDatos(rol.id(), 2, 250, lane.version())))).andExpect(status().isOk());
        mvc.perform(get(rutaLanes).principal(admin::getCorreo)).andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
        var datos = new NodoDatos(ClaseNodo.ACTIVIDAD, propietario, "Actividad HTTP", 100, 100, 100, 50,
                lane.id(), TipoActividad.TAREA, null, null, null, null, null);
        mvc.perform(post(raiz + "/nodos").principal(editor::getCorreo).contentType("application/json")
                .content(json.writeValueAsString(datos))).andExpect(status().isCreated());
        var nodo = nodos.listar(admin.getId(), proceso.getId()).stream()
                .filter(n -> n.nombre().equals("Actividad HTTP")).findFirst().orElseThrow();
        mvc.perform(put(raiz + "/nodos/" + nodo.id()).principal(editor::getCorreo).contentType("application/json")
                .content(json.writeValueAsString(datosNodo(nodo, "Actividad editada HTTP")))).andExpect(status().isOk());
        var inicio = detalle.nodos().stream().filter(n -> n.tipoEvento() == TipoEvento.INICIO).findFirst().orElseThrow();
        var conexion = new ArcoDatos(inicio.id(), nodo.id(), "HTTP", null);
        mvc.perform(post(raiz + "/arcos").principal(editor::getCorreo).contentType("application/json")
                .content(json.writeValueAsString(conexion))).andExpect(status().isCreated());
        var arco = arcoRepo.buscarPar(inicio.id(), nodo.id()).orElseThrow();
        mvc.perform(put(raiz + "/arcos/" + arco.getId()).principal(editor::getCorreo).contentType("application/json")
                .content(json.writeValueAsString(conexion))).andExpect(status().isOk());
        mvc.perform(delete(raiz + "/arcos/" + arco.getId()).principal(admin::getCorreo).contentType("application/json")
                .content(json.writeValueAsString(new Retirada(proceso.getVersion(), true)))).andExpect(status().isOk());
        mvc.perform(delete(raiz + "/nodos/" + nodo.id()).principal(admin::getCorreo).contentType("application/json")
                .content(json.writeValueAsString(new Retirada(proceso.getVersion(), true)))).andExpect(status().isOk());
        mvc.perform(delete(rutaLanes + "/" + lane.id()).principal(admin::getCorreo).contentType("application/json")
                .content(json.writeValueAsString(new Retirada(laneRepo.findById(lane.id()).orElseThrow().getVersion(), true))))
                .andExpect(status().isNoContent());
        mvc.perform(delete(raiz + "/pools/" + pool.id()).principal(admin::getCorreo).contentType("application/json")
                .content(json.writeValueAsString(new Retirada(pools.listar(admin.getId(), proceso.getId()).stream()
                        .filter(p -> p.id().equals(pool.id())).findFirst().orElseThrow().version(), true))))
                .andExpect(status().isNoContent());
        mvc.perform(delete("/roles-proceso/" + rol.id()).principal(admin::getCorreo).contentType("application/json")
                .content(json.writeValueAsString(new Retirada(rolRepo.findById(rol.id()).orElseThrow().getVersion(), true))))
                .andExpect(status().isNoContent());
    }

    @Test
    void endpointsDanDetalleSinCredencialesYExigenSesion() throws Exception {
        mvc.perform(get("/procesos")).andExpect(status().isUnauthorized());
        mvc.perform(get("/procesos/" + proceso.getId() + "/diagrama").principal(admin::getCorreo))
                .andExpect(status().isOk()).andExpect(jsonPath("$.nodos.length()").value(7))
                .andExpect(jsonPath("$.mensajes.length()").value(2));
        for (String ruta : List.of("/pools", "/nodos", "/arcos"))
            mvc.perform(get("/procesos/" + proceso.getId() + ruta).principal(admin::getCorreo)).andExpect(status().isOk());
        mvc.perform(get("/roles-proceso").principal(admin::getCorreo)).andExpect(status().isOk());
        mvc.perform(get("/procesos/" + proceso.getId() + "/historial").principal(admin::getCorreo)).andExpect(status().isOk());
    }

    @Test
    void validaDtosPorHttpYConectaLaEdicionConElServicio() throws Exception {
        mvc.perform(post("/procesos").principal(admin::getCorreo).contentType("application/json").content("{}"))
                .andExpect(status().isBadRequest());
        mvc.perform(put("/procesos/" + proceso.getId()).principal(editor::getCorreo).contentType("application/json")
                .content(json.writeValueAsString(datosProceso(EstadoPublicacion.BORRADOR))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(proceso.getId()));
        mvc.perform(delete("/procesos/" + proceso.getId()).principal(admin::getCorreo).contentType("application/json")
                .content(json.writeValueAsString(Map.of("version", proceso.getVersion(), "confirmar", false))))
                .andExpect(status().isBadRequest());
    }

    private Datos datosProceso(EstadoPublicacion estado) {
        return new Datos(proceso.getNombre(), proceso.getDescripcion(), proceso.getCategoria(), estado, proceso.getVersion());
    }

    private NodoVista tarea() {
        return detalle.nodos().stream().filter(n -> n.tipoActividad() == TipoActividad.TAREA).findFirst().orElseThrow();
    }

    private NodoDatos datosNodo(NodoVista n, String nombre) {
        return new NodoDatos(n.clase(), n.poolId(), nombre, n.posicionX(), n.posicionY(), n.ancho(), n.alto(),
                n.laneId(), n.tipoActividad(), n.tipoGateway(), n.direccionGateway(), n.tipoEvento(), n.naturalezaEvento(), n.operacionMensaje());
    }

    private PoolDatos datosPool() {
        return new PoolDatos("Otro participante", TipoParticipante.SISTEMA_EXTERNO, true, 0, 700, 300, 150, 3, null);
    }
}
