package com.proyecto.inicio.service;

import com.proyecto.inicio.dto.request.*;
import com.proyecto.inicio.dto.MensajeDto.*;
import com.proyecto.inicio.dto.FlujoMensajeDto.*;
import com.proyecto.inicio.dto.PermisoEstructuraDto.*;
import com.proyecto.inicio.dto.MensajeDto.MensajeResponseDto;
import com.proyecto.inicio.entity.*;
import com.proyecto.inicio.entity.enums.*;
import com.proyecto.inicio.exception.AccesoColaboracionException;
import com.proyecto.inicio.repository.*;
import jakarta.persistence.EntityManager;
import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.test.context.jdbc.Sql;

import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@Transactional
@Sql(scripts = "/esquema-nodos-mensajeria.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_CLASS)
class MensajeriaServiceTest {
    @Autowired EntityManager em;
    @Autowired MensajeService mensajes;
    @Autowired FlujoMensajeService flujos;
    @Autowired PermisoEstructuraService permisos;
    @Autowired MensajeRepository mensajeRepository;
    @Autowired CampoMensajeRepository campoRepository;
    @Autowired FlujoMensajeRepository flujoRepository;
    @Autowired HistorialCambioRepository historial;
    Empresa empresa;
    Usuario admin, editor, lector;
    Proceso proceso;
    Pool origen, destino;
    Evento envio, recepcion;
    Actividad actividad;

    @BeforeEach
    void prepararDiagrama() {
        empresa = Empresa.builder().nombre("Empresa prueba").nit(UUID.randomUUID().toString())
                .correoContacto("contacto@ejemplo.co").activo(true).build();
        em.persist(empresa);
        admin = usuario(RolAcceso.ADMINISTRADOR);
        editor = usuario(RolAcceso.EDITOR);
        lector = usuario(RolAcceso.LECTURA);
        proceso = Proceso.builder().empresa(empresa).creador(admin).nombre("Proceso " + UUID.randomUUID())
                .descripcion("Prueba de colaboración").categoria("Pruebas").activo(true).build();
        em.persist(proceso);
        origen = pool(false);
        destino = pool(false);
        envio = evento(origen, OperacionEventoMensaje.ENVIO);
        recepcion = evento(destino, OperacionEventoMensaje.RECEPCION);
        RolProceso rol = RolProceso.builder().empresa(empresa).nombre("Analista").activo(true).build();
        em.persist(rol);
        Lane lane = Lane.builder().pool(destino).rolProceso(rol).orden(1).altura(100).activo(true).build();
        em.persist(lane);
        actividad = new Actividad();
        completarNodo(actividad, destino);
        actividad.setTipo(TipoActividad.TAREA); actividad.setLane(lane);
        em.persist(actividad);
        em.flush();
    }

    @Test
    void persisteContratoYFlujoConConsultasJPQLYAuditoria() {
        var salida = crear(envio, SentidoMensaje.ENVIO);
        var entrada = crear(recepcion, SentidoMensaje.RECEPCION);
        mensajes.crearCampo(admin.getId(), proceso.getId(), salida.getId(), campo("radicado"));
        mensajes.crearCampo(admin.getId(), proceso.getId(), entrada.getId(), campo("radicado"));
        var flujo = flujos.crear(editor.getId(), proceso.getId(), conexion(origen, destino, envio, recepcion));
        em.flush(); em.clear();
        assertThat(mensajes.listar(admin.getId(), proceso.getId())).hasSize(2);
        assertThat(flujos.listar(admin.getId(), proceso.getId())).extracting("id").containsExactly(flujo.getId());
        assertThat(mensajes.advertencias(admin.getId(), proceso.getId())).isEmpty();
        assertThat(historial.findByEmpresaContextoId(empresa.getId())).hasSize(5);
    }

    @Test
    void rechazaFlujosEnUnSoloPoolYOtrosProcesos() {
        crear(envio, SentidoMensaje.ENVIO);
        crear(recepcion, SentidoMensaje.RECEPCION);
        assertThatThrownBy(() -> flujos.crear(admin.getId(), proceso.getId(), conexion(origen, origen, envio, recepcion)))
                .isInstanceOf(IllegalArgumentException.class);
        Proceso otro = Proceso.builder().empresa(empresa).creador(admin).nombre("Otro").activo(true).build();
        em.persist(otro);
        destino.setProceso(otro); em.flush();
        assertThatThrownBy(() -> flujos.crear(admin.getId(), proceso.getId(), conexion(origen, destino, envio, recepcion)))
                .isInstanceOf(jakarta.persistence.EntityNotFoundException.class);
    }

    @Test
    void validaDatosYNoPermiteCrearMensajesEnTareasNormales() {
        var datos = solicitud(actividad, SentidoMensaje.ENVIO);
        assertThatThrownBy(() -> mensajes.crear(admin.getId(), proceso.getId(), datos)).isInstanceOf(IllegalArgumentException.class);
        datos.setNombre(" ");
        assertThatThrownBy(() -> mensajes.crear(admin.getId(), proceso.getId(), datos)).isInstanceOf(ConstraintViolationException.class);
        assertThat(mensajeRepository.count()).isZero();
    }

    @Test
    void lectorNoModificaYUsuarioAjenoNoConsulta() {
        assertThatThrownBy(() -> mensajes.crear(lector.getId(), proceso.getId(), solicitud(envio, SentidoMensaje.ENVIO)))
                .isInstanceOf(AccesoColaboracionException.class);
        Empresa ajena = Empresa.builder().nombre("Ajena").nit(UUID.randomUUID().toString()).correoContacto("a@b.co").activo(true).build();
        em.persist(ajena);
        editor.setEmpresa(ajena); em.flush();
        assertThatThrownBy(() -> mensajes.listar(editor.getId(), proceso.getId())).isInstanceOf(AccesoColaboracionException.class);
        admin.setEstado(EstadoUsuario.INACTIVO); em.flush();
        assertThatThrownBy(() -> mensajes.listar(admin.getId(), proceso.getId())).isInstanceOf(AccesoColaboracionException.class);
    }

    @Test
    void correlacionExigeCampoPropioYProtegeSuRetirada() {
        var salida = crear(envio, SentidoMensaje.ENVIO);
        var entrada = crear(recepcion, SentidoMensaje.RECEPCION);
        var campoAjeno = mensajes.crearCampo(admin.getId(), proceso.getId(), entrada.getId(), campo("ajeno"));
        var datos = solicitud(envio, SentidoMensaje.ENVIO);
        datos.setVersion(salida.getVersion()); datos.setCorrelacionTipo(TipoCorrelacion.CAMPO);
        datos.setCorrelacionNegocio(null); datos.setCorrelacionCampoId(campoAjeno.getId());
        assertThatThrownBy(() -> mensajes.actualizar(admin.getId(), proceso.getId(), salida.getId(), datos))
                .isInstanceOf(IllegalArgumentException.class);
        var propio = mensajes.crearCampo(admin.getId(), proceso.getId(), salida.getId(), campo("radicado"));
        datos.setCorrelacionCampoId(propio.getId());
        mensajes.actualizar(admin.getId(), proceso.getId(), salida.getId(), datos);
        assertThatThrownBy(() -> mensajes.retirarCampo(admin.getId(), proceso.getId(), salida.getId(), propio.getId(), propio.getVersion(), true))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void nombreDeCampoUnicoYVersionObsoletaSeRechazan() {
        var salida = crear(envio, SentidoMensaje.ENVIO);
        mensajes.crearCampo(admin.getId(), proceso.getId(), salida.getId(), campo("radicado"));
        assertThatThrownBy(() -> mensajes.crearCampo(admin.getId(), proceso.getId(), salida.getId(), campo(" radicado ")))
                .isInstanceOf(IllegalStateException.class);
        var datos = solicitud(envio, SentidoMensaje.ENVIO);
        datos.setVersion(salida.getVersion()); datos.setNombre("Renombrado");
        mensajes.actualizar(admin.getId(), proceso.getId(), salida.getId(), datos);
        assertThatThrownBy(() -> mensajes.actualizar(admin.getId(), proceso.getId(), salida.getId(), datos))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void retirarMensajeConservaCamposYFlujosExternos() {
        var salida = crear(envio, SentidoMensaje.ENVIO);
        var campo = mensajes.crearCampo(admin.getId(), proceso.getId(), salida.getId(), campo("radicado"));
        Pool externo = pool(true);
        var solicitud = conexion(origen, externo, envio, null);
        solicitud.setTipoDestino(TipoDestinoExterno.CORREO); solicitud.setPoliticaFallo(PoliticaFallo.CONTINUAR);
        var flujo = flujos.crear(admin.getId(), proceso.getId(), solicitud);
        assertThatThrownBy(() -> mensajes.retirar(admin.getId(), proceso.getId(), salida.getId(), salida.getVersion(), false))
                .isInstanceOf(IllegalArgumentException.class);
        mensajes.retirar(admin.getId(), proceso.getId(), salida.getId(), salida.getVersion(), true);
        em.flush(); em.clear();
        assertThat(mensajeRepository.findById(salida.getId()).orElseThrow().getActivo()).isFalse();
        assertThat(campoRepository.findById(campo.getId()).orElseThrow().getActivo()).isFalse();
        assertThat(flujoRepository.findById(flujo.getId()).orElseThrow().getActivo()).isFalse();
        assertThat(mensajes.listar(admin.getId(), proceso.getId())).isEmpty();
    }

    @Test
    void usoSoloAsociaActividadesDelPoolReceptorYPuedeReactivarse() {
        var entrada = crear(recepcion, SentidoMensaje.RECEPCION);
        var uso = mensajes.crearUso(editor.getId(), proceso.getId(), entrada.getId(), new UsoMensajeActividadRequestDto(actividad.getId()));
        mensajes.retirarUso(admin.getId(), proceso.getId(), entrada.getId(), uso.getId(), uso.getVersion(), true);
        em.flush();
        assertThat(mensajes.crearUso(admin.getId(), proceso.getId(), entrada.getId(), new UsoMensajeActividadRequestDto(actividad.getId())).getId())
                .isEqualTo(uso.getId());
        var salida = crear(envio, SentidoMensaje.ENVIO);
        assertThatThrownBy(() -> mensajes.crearUso(admin.getId(), proceso.getId(), salida.getId(), new UsoMensajeActividadRequestDto(actividad.getId())))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void permisosRespetanAdministradorEditorYLector() {
        assertThat(permisos.permitido(admin.getId(), proceso.getId(), RecursoEstructura.LANE, AccionEstructura.CREAR)).isTrue();
        assertThat(permisos.permitido(editor.getId(), proceso.getId(), RecursoEstructura.LANE, AccionEstructura.CREAR)).isFalse();
        var datos = new PermisoEstructuraRequestDto(RolAcceso.EDITOR, RecursoEstructura.LANE, AccionEstructura.CREAR, true, null);
        var regla = permisos.guardar(admin.getId(), proceso.getId(), datos);
        assertThat(permisos.permitido(editor.getId(), proceso.getId(), RecursoEstructura.LANE, AccionEstructura.CREAR)).isTrue();
        datos.setPermitido(false); datos.setVersion(regla.getVersion());
        permisos.guardar(admin.getId(), proceso.getId(), datos);
        assertThat(permisos.permitido(editor.getId(), proceso.getId(), RecursoEstructura.LANE, AccionEstructura.CREAR)).isFalse();
        datos.setRolAcceso(RolAcceso.LECTURA); datos.setPermitido(true);
        assertThatThrownBy(() -> permisos.guardar(admin.getId(), proceso.getId(), datos)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> permisos.guardar(editor.getId(), proceso.getId(), datos)).isInstanceOf(AccesoColaboracionException.class);
    }

    @Test
    void advierteContratosIncompatiblesSinImpedirGuardarBorrador() {
        var salida = crear(envio, SentidoMensaje.ENVIO);
        var datos = solicitud(recepcion, SentidoMensaje.RECEPCION);
        datos.setNombre("Otro nombre"); datos.setCorrelacionTipo(null); datos.setCorrelacionNegocio(null);
        mensajes.crear(admin.getId(), proceso.getId(), datos);
        mensajes.crearCampo(admin.getId(), proceso.getId(), salida.getId(), campo("radicado"));
        flujos.crear(admin.getId(), proceso.getId(), conexion(origen, destino, envio, recepcion));
        em.flush(); em.clear();
        assertThat(mensajes.advertencias(admin.getId(), proceso.getId()))
                .anyMatch(a -> a.contains("nombre")).anyMatch(a -> a.contains("campos")).anyMatch(a -> a.contains("correlación"));
    }

    private MensajeResponseDto crear(Nodo nodo, SentidoMensaje sentido) {
        return mensajes.crear(admin.getId(), proceso.getId(), solicitud(nodo, sentido));
    }

    private MensajeRequestDto solicitud(Nodo nodo, SentidoMensaje sentido) {
        return new MensajeRequestDto(nodo.getId(), sentido, "Solicitud", false, TipoCorrelacion.NEGOCIO, "radicado", null,
                sentido == SentidoMensaje.RECEPCION ? PoliticaSinCorrespondencia.DESCARTAR : null, null);
    }

    private CampoMensajeRequestDto campo(String nombre) { return new CampoMensajeRequestDto(nombre, TipoDatoMensaje.TEXTO, 0, null); }

    private FlujoMensajeRequestDto conexion(Pool a, Pool b, Nodo o, Nodo d) {
        return new FlujoMensajeRequestDto(a.getId(), b.getId(), o == null ? null : o.getId(), d == null ? null : d.getId(), null, null, null, null);
    }

    private Usuario usuario(RolAcceso rol) {
        Usuario usuario = Usuario.builder().empresa(empresa).nombre("Usuario").correo(UUID.randomUUID() + "@ejemplo.co")
                .rolAcceso(rol).estado(EstadoUsuario.ACTIVO).build();
        em.persist(usuario);
        return usuario;
    }

    private Pool pool(boolean cajaNegra) {
        Pool pool = Pool.builder().proceso(proceso).nombre("Participante").tipoParticipante(TipoParticipante.EMPRESA)
                .propietario(false).cajaNegra(cajaNegra).posicionX(0).posicionY(0).ancho(800).alto(400).orden(1).activo(true).build();
        em.persist(pool);
        return pool;
    }

    private Evento evento(Pool pool, OperacionEventoMensaje operacion) {
        Evento evento = new Evento();
        completarNodo(evento, pool);
        evento.setTipo(TipoEvento.INTERMEDIO); evento.setNaturaleza(NaturalezaEvento.MENSAJE); evento.setOperacionMensaje(operacion);
        em.persist(evento);
        return evento;
    }

    private void completarNodo(Nodo nodo, Pool pool) {
        nodo.setProceso(proceso); nodo.setPool(pool); nodo.setNombre("Nodo");
        nodo.setPosicionX(10); nodo.setPosicionY(10); nodo.setAncho(100); nodo.setAlto(60);
    }
}
