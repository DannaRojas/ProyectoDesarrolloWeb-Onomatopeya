package com.proyecto.inicio.config;

import com.proyecto.inicio.dto.request.ArcoDatos;
import com.proyecto.inicio.dto.request.CampoMensajeRequestDto;
import com.proyecto.inicio.dto.request.FlujoMensajeRequestDto;
import com.proyecto.inicio.dto.request.MensajeRequestDto;
import com.proyecto.inicio.dto.request.NodoDatos;
import com.proyecto.inicio.dto.request.PoolDatos;
import com.proyecto.inicio.dto.request.ProcesoRequestDto;
import com.proyecto.inicio.dto.request.UsoMensajeActividadRequestDto;
import com.proyecto.inicio.dto.response.MensajeResponseDto;
import com.proyecto.inicio.dto.response.NodoVista;
import com.proyecto.inicio.dto.response.PoolVista;
import com.proyecto.inicio.dto.response.ProcesoResponseDto;
import com.proyecto.inicio.entity.Empresa;
import com.proyecto.inicio.entity.Lane;
import com.proyecto.inicio.entity.Pool;
import com.proyecto.inicio.entity.Proceso;
import com.proyecto.inicio.entity.RolProceso;
import com.proyecto.inicio.entity.Usuario;
import com.proyecto.inicio.entity.enums.ClaseNodo;
import com.proyecto.inicio.entity.enums.DireccionGateway;
import com.proyecto.inicio.entity.enums.EstadoUsuario;
import com.proyecto.inicio.entity.enums.NaturalezaEvento;
import com.proyecto.inicio.entity.enums.OperacionEventoMensaje;
import com.proyecto.inicio.entity.enums.PoliticaFallo;
import com.proyecto.inicio.entity.enums.PoliticaSinCorrespondencia;
import com.proyecto.inicio.entity.enums.RolAcceso;
import com.proyecto.inicio.entity.enums.SentidoMensaje;
import com.proyecto.inicio.entity.enums.TipoActividad;
import com.proyecto.inicio.entity.enums.TipoCorrelacion;
import com.proyecto.inicio.entity.enums.TipoDatoMensaje;
import com.proyecto.inicio.entity.enums.TipoDestinoExterno;
import com.proyecto.inicio.entity.enums.TipoEvento;
import com.proyecto.inicio.entity.enums.TipoGateway;
import com.proyecto.inicio.entity.enums.TipoParticipante;
import com.proyecto.inicio.repository.EmpresaRepository;
import com.proyecto.inicio.repository.LaneRepository;
import com.proyecto.inicio.repository.NodoRepository;
import com.proyecto.inicio.repository.PoolRepository;
import com.proyecto.inicio.repository.ProcesoRepository;
import com.proyecto.inicio.repository.RolProcesoRepository;
import com.proyecto.inicio.repository.UsuarioRepository;
import com.proyecto.inicio.service.ArcoService;
import com.proyecto.inicio.service.FlujoMensajeService;
import com.proyecto.inicio.service.MensajeService;
import com.proyecto.inicio.service.NodoService;
import com.proyecto.inicio.service.PoolService;
import com.proyecto.inicio.service.ProcesoService;

import lombok.RequiredArgsConstructor;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.datos-demo", havingValue = "true")
public class DatosIniciales implements CommandLineRunner {
    private final EmpresaRepository empresas;
    private final UsuarioRepository usuarios;
    private final ProcesoRepository procesos;
    private final RolProcesoRepository roles;
    private final PoolRepository pools;
    private final LaneRepository lanes;
    private final ProcesoService procesoService;
    private final PoolService poolService;
    private final NodoService nodoService;
    private final ArcoService arcoService;
    private final MensajeService mensajeService;
    private final FlujoMensajeService flujoService;
    private final NodoRepository nodos;
    private final PasswordEncoder codificador;

    @Override
    @Transactional
    public void run(String... args) {
        prepararEmpresa("DEMO-001", "Empresa Demo Uno", "uno");
        prepararEmpresa("DEMO-002", "Empresa Demo Dos", "dos");
    }

    private void prepararEmpresa(String nit, String nombre, String sufijo) {
        Empresa empresa =
                empresas.findByNit(nit)
                        .orElseGet(
                                () ->
                                        empresas.save(
                                                Empresa.builder()
                                                        .nit(nit)
                                                        .nombre(nombre)
                                                        .correoContacto(
                                                                "contacto."
                                                                        + sufijo
                                                                        + "@example.com")
                                                        .activo(true)
                                                        .build()));
        if (!nombre.equals(empresa.getNombre()) || !Boolean.TRUE.equals(empresa.getActivo())) {
            throw new IllegalStateException(
                    "El NIT de demostración ya pertenece a otra empresa o está inactivo.");
        }
        Usuario admin =
                usuario(empresa, "Administrador demo", "admin." + sufijo, RolAcceso.ADMINISTRADOR);
        usuario(empresa, "Editor demo", "editor." + sufijo, RolAcceso.EDITOR);
        usuario(empresa, "Lector demo", "lector." + sufijo, RolAcceso.LECTURA);

        Proceso proceso =
                procesos.findByEmpresaIdAndActivoTrue(empresa.getId()).stream()
                        .filter(p -> p.getNombre().equals("Solicitud de compra demo"))
                        .findFirst()
                        .orElseGet(
                                () -> {
                                    if (procesos.existsByEmpresaIdAndNombre(
                                            empresa.getId(), "Solicitud de compra demo")) {
                                        throw new IllegalStateException(
                                                "El proceso de demostración está retirado; no se"
                                                    + " reactiva.");
                                    }
                                    ProcesoResponseDto respuesta =
                                            procesoService.crear(
                                                    new ProcesoRequestDto(
                                                            empresa.getId(),
                                                            admin.getId(),
                                                            "Solicitud de compra demo",
                                                            "Proceso ficticio para probar la"
                                                                + " entrega.",
                                                            "Compras"));
                                    return procesos.findById(respuesta.getId()).orElseThrow();
                                });
        RolProceso rol =
                roles.findByEmpresaIdAndActivoTrue(empresa.getId()).stream()
                        .filter(r -> r.getNombre().equals("Analista demo"))
                        .findFirst()
                        .orElseGet(
                                () -> {
                                    if (roles.existsByEmpresaIdAndNombre(
                                            empresa.getId(), "Analista demo")) {
                                        throw new IllegalStateException(
                                                "El rol de demostración está retirado; no se"
                                                    + " reactiva.");
                                    }
                                    return roles.save(
                                            RolProceso.builder()
                                                    .empresa(empresa)
                                                    .nombre("Analista demo")
                                                    .descripcion("Revisa solicitudes de ejemplo.")
                                                    .activo(true)
                                                    .build());
                                });
        Pool pool =
                pools.findByProcesoIdAndActivoTrue(proceso.getId()).stream()
                        .filter(p -> Boolean.TRUE.equals(p.getPropietario()))
                        .findFirst()
                        .orElseThrow(
                                () ->
                                        new IllegalStateException(
                                                "El proceso demo no tiene pool propietario"
                                                    + " activo."));
        if (lanes.findByPoolIdAndActivoTrueOrderByOrden(pool.getId()).isEmpty()) {
            if (lanes.findAll().stream().anyMatch(l -> l.getPool().getId().equals(pool.getId()))) {
                throw new IllegalStateException(
                        "El pool demo ya tiene lanes retiradas; no se reemplazan.");
            }
            lanes.save(
                    Lane.builder()
                            .pool(pool)
                            .rolProceso(rol)
                            .orden(1)
                            .altura(300)
                            .activo(true)
                            .build());
        }
        prepararDiagrama(admin, proceso, pool);
    }

    private void prepararDiagrama(Usuario admin, Proceso proceso, Pool propietario) {
        // Si alguien ya trabajó sobre el diagrama, no lo reconstruimos ni reemplazamos sus
        // elementos.
        if (!nodos.listarVigentes(proceso.getId()).isEmpty()
                || nodos.findAll().stream()
                        .anyMatch(n -> n.getProceso().getId().equals(proceso.getId()))) return;
        Long actorId = admin.getId();
        Long procesoId = proceso.getId();
        Long poolId = propietario.getId();
        Long laneId = lanes.findByPoolIdAndActivoTrueOrderByOrden(poolId).get(0).getId();
        PoolVista externo =
                poolService.crear(
                        actorId,
                        procesoId,
                        new PoolDatos(
                                "Servicio externo demo",
                                TipoParticipante.SISTEMA_EXTERNO,
                                true,
                                0,
                                700,
                                1000,
                                150,
                                2,
                                null));
        NodoVista inicio =
                nodoService.crear(
                        actorId,
                        procesoId,
                        evento(poolId, "Inicio demo", 50, TipoEvento.INICIO,
                                NaturalezaEvento.NORMAL, null));
        NodoVista recepcion =
                nodoService.crear(
                        actorId,
                        procesoId,
                        evento(poolId, "Recibir solicitud demo", 180, TipoEvento.INTERMEDIO,
                                NaturalezaEvento.MENSAJE, OperacionEventoMensaje.RECEPCION));
        NodoVista envio =
                nodoService.crear(
                        actorId,
                        procesoId,
                        actividad(poolId, laneId, "Notificar revisión demo", 310, TipoActividad.ENVIO));
        NodoVista decision =
                nodoService.crear(
                        actorId,
                        procesoId,
                        gateway(poolId, "Decidir compra demo", 440,
                                TipoGateway.EXCLUSIVO, DireccionGateway.DIVERGENCIA));
        NodoVista aprobar =
                nodoService.crear(
                        actorId,
                        procesoId,
                        actividad(poolId, laneId, "Aprobar compra demo", 570, TipoActividad.TAREA));
        NodoVista rechazar =
                nodoService.crear(
                        actorId,
                        procesoId,
                        actividad(poolId, laneId, "Rechazar compra demo", 700, TipoActividad.TAREA));
        NodoVista fin =
                nodoService.crear(
                        actorId,
                        procesoId,
                        evento(poolId, "Fin demo", 830, TipoEvento.FIN,
                                NaturalezaEvento.NORMAL, null));
        conectar(actorId, procesoId, inicio.id(), recepcion.id(), null);
        conectar(actorId, procesoId, recepcion.id(), envio.id(), null);
        conectar(actorId, procesoId, envio.id(), decision.id(), null);
        conectar(actorId, procesoId, decision.id(), aprobar.id(), "monto <= 1000");
        conectar(actorId, procesoId, decision.id(), rechazar.id(), "monto > 1000");
        conectar(actorId, procesoId, aprobar.id(), fin.id(), null);
        conectar(actorId, procesoId, rechazar.id(), fin.id(), null);
        MensajeResponseDto entrada =
                mensajeService.crear(
                        actorId,
                        procesoId,
                        new MensajeRequestDto(
                                recepcion.id(),
                                SentidoMensaje.RECEPCION,
                                "SolicitudCompraDemo",
                                true,
                                TipoCorrelacion.NEGOCIO,
                                "numeroSolicitud",
                                null,
                                PoliticaSinCorrespondencia.DESCARTAR,
                                null));
        MensajeResponseDto salida =
                mensajeService.crear(
                        actorId,
                        procesoId,
                        new MensajeRequestDto(
                                envio.id(),
                                SentidoMensaje.ENVIO,
                                "RevisionCompraDemo",
                                false,
                                TipoCorrelacion.NEGOCIO,
                                "numeroSolicitud",
                                null,
                                null,
                                null));
        mensajeService.crearCampo(
                actorId,
                procesoId,
                entrada.getId(),
                new CampoMensajeRequestDto("numeroSolicitud", TipoDatoMensaje.TEXTO, 0, null));
        mensajeService.crearCampo(
                actorId,
                procesoId,
                salida.getId(),
                new CampoMensajeRequestDto("numeroSolicitud", TipoDatoMensaje.TEXTO, 0, null));
        mensajeService.crearUso(
                actorId, procesoId, entrada.getId(), new UsoMensajeActividadRequestDto(envio.id()));
        flujoService.crear(
                actorId,
                procesoId,
                new FlujoMensajeRequestDto(
                        externo.id(), poolId, null, recepcion.id(), null, null, null, null));
        flujoService.crear(
                actorId,
                procesoId,
                new FlujoMensajeRequestDto(
                        poolId,
                        externo.id(),
                        envio.id(),
                        null,
                        TipoDestinoExterno.CORREO,
                        PoliticaFallo.CONTINUAR,
                        null,
                        null));
    }

    private NodoDatos evento(
            Long poolId, String nombre, int x, TipoEvento tipo,
            NaturalezaEvento naturaleza, OperacionEventoMensaje operacion) {
        return new NodoDatos(
                ClaseNodo.EVENTO, poolId, nombre, x, 150, 100, 60,
                null, null, null, null, tipo, naturaleza, operacion);
    }

    private NodoDatos actividad(
            Long poolId, Long laneId, String nombre, int x, TipoActividad tipo) {
        return new NodoDatos(
                ClaseNodo.ACTIVIDAD, poolId, nombre, x, 150, 100, 60,
                laneId, tipo, null, null, null, null, null);
    }

    private NodoDatos gateway(
            Long poolId, String nombre, int x, TipoGateway tipo,
            DireccionGateway direccion) {
        return new NodoDatos(
                ClaseNodo.GATEWAY, poolId, nombre, x, 150, 100, 60,
                null, null, tipo, direccion, null, null, null);
    }

    private void conectar(
            Long actorId, Long procesoId, Long origen, Long destino, String condicion) {
        arcoService.crear(actorId, procesoId, new ArcoDatos(origen, destino, null, condicion));
    }

    private Usuario usuario(Empresa empresa, String nombre, String prefijo, RolAcceso rol) {
        String correo = prefijo + "@example.com";
        Usuario existente = usuarios.findByCorreo(correo).orElse(null);
        if (existente != null) {
            if (!existente.getEmpresa().getId().equals(empresa.getId())) {
                throw new IllegalStateException("El correo demo pertenece a otra empresa.");
            }
            return existente;
        }
        // Esta contraseña es únicamente para las cuentas ficticias de la demostración.
        String hash = codificador.encode("DemoEntrega1!2026");
        return usuarios.save(
                Usuario.builder()
                        .empresa(empresa)
                        .nombre(nombre)
                        .correo(correo)
                        .passwordHash(hash)
                        .rolAcceso(rol)
                        .estado(EstadoUsuario.ACTIVO)
                        .build());
    }
}
