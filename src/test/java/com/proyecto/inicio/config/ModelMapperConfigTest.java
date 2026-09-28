package com.proyecto.inicio.config;

import com.proyecto.inicio.dto.request.*;
import com.proyecto.inicio.dto.MensajeDto.*;
import com.proyecto.inicio.dto.FlujoMensajeDto.*;
import com.proyecto.inicio.dto.PermisoEstructuraDto.*;
import com.proyecto.inicio.dto.response.*;
import com.proyecto.inicio.entity.*;
import com.proyecto.inicio.entity.enums.*;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.modelmapper.ModelMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

class ModelMapperConfigTest {
    static ModelMapper mapper;

    @BeforeAll
    static void configurar() {
        mapper = new Configuracion().modelMapper();
    }

    @Test
    void todosLosCamposDeDestinoEstanMapeadosOExcluidos() {
        assertThatCode(mapper::validate).doesNotThrowAnyException();
    }

    @Test
    void mensajeSeConvierteConIdsDeRelacionesYCamposOpcionales() {
        Evento nodo = new Evento(); nodo.setId(10L);
        CampoMensaje campo = CampoMensaje.builder().id(20L).build();
        Mensaje mensaje = Mensaje.builder().id(30L).nodo(nodo).sentido(SentidoMensaje.RECEPCION).nombre("Solicitud")
                .origenExterno(true).correlacionTipo(TipoCorrelacion.CAMPO).correlacionCampo(campo)
                .politicaSinCorrespondencia(PoliticaSinCorrespondencia.DESCARTAR).activo(true).version(2L).build();
        var esperado = new MensajeResponseDto(30L, 10L, SentidoMensaje.RECEPCION, "Solicitud", true,
                TipoCorrelacion.CAMPO, null, 20L, PoliticaSinCorrespondencia.DESCARTAR, true, 2L);
        assertThat(mapper.map(mensaje, MensajeResponseDto.class)).usingRecursiveComparison().isEqualTo(esperado);
        mensaje.setCorrelacionCampo(null); mensaje.setCorrelacionTipo(null);
        assertThat(mapper.map(mensaje, MensajeResponseDto.class).getCorrelacionCampoId()).isNull();
    }

    @Test
    void respuestasDeCamposUsosYPermisosConservanSusDatos() {
        Mensaje mensaje = Mensaje.builder().id(10L).build();
        CampoMensaje campo = CampoMensaje.builder().id(20L).mensaje(mensaje).nombre("radicado")
                .tipoDato(TipoDatoMensaje.TEXTO).orden(3).activo(false).version(4L).build();
        assertThat(mapper.map(campo, CampoMensajeResponseDto.class)).usingRecursiveComparison()
                .isEqualTo(new CampoMensajeResponseDto(20L, 10L, "radicado", TipoDatoMensaje.TEXTO, 3, false, 4L));

        Actividad actividad = new Actividad(); actividad.setId(30L);
        UsoMensajeActividad uso = UsoMensajeActividad.builder().id(40L).mensaje(mensaje).actividad(actividad)
                .activo(true).version(5L).build();
        assertThat(mapper.map(uso, UsoMensajeActividadResponseDto.class)).usingRecursiveComparison()
                .isEqualTo(new UsoMensajeActividadResponseDto(40L, 10L, 30L, true, 5L));

        Proceso proceso = Proceso.builder().id(50L).build();
        PermisoEstructura permiso = PermisoEstructura.builder().id(60L).proceso(proceso).rolAcceso(RolAcceso.EDITOR)
                .recurso(RecursoEstructura.LANE).accion(AccionEstructura.EDITAR).permitido(false).version(6L).build();
        assertThat(mapper.map(permiso, PermisoEstructuraResponseDto.class)).usingRecursiveComparison()
                .isEqualTo(new PermisoEstructuraResponseDto(60L, 50L, RolAcceso.EDITOR, RecursoEstructura.LANE,
                        AccionEstructura.EDITAR, false, 6L));
    }

    @Test
    void respuestaDeFlujoAdmiteExtremosOpcionalesSinPerderOtrosIds() {
        Proceso proceso = Proceso.builder().id(10L).build();
        Pool origen = Pool.builder().id(20L).build(), destino = Pool.builder().id(30L).build();
        Evento nodo = new Evento(); nodo.setId(40L);
        Actividad error = new Actividad(); error.setId(50L);
        FlujoMensaje flujo = FlujoMensaje.builder().id(60L).proceso(proceso).poolOrigen(origen).poolDestino(destino)
                .nodoOrigen(nodo).tipoDestino(TipoDestinoExterno.CORREO).politicaFallo(PoliticaFallo.DERIVAR)
                .actividadError(error).activo(true).version(2L).build();
        assertThat(mapper.map(flujo, FlujoMensajeResponseDto.class)).usingRecursiveComparison()
                .isEqualTo(new FlujoMensajeResponseDto(60L, 10L, 20L, 30L, 40L, null,
                        TipoDestinoExterno.CORREO, PoliticaFallo.DERIVAR, 50L, true, 2L));
        flujo.setNodoOrigen(null); flujo.setNodoDestino(nodo); flujo.setActividadError(null);
        flujo.setTipoDestino(null); flujo.setPoliticaFallo(null);
        var respuesta = mapper.map(flujo, FlujoMensajeResponseDto.class);
        assertThat(respuesta.getNodoOrigenId()).isNull();
        assertThat(respuesta.getNodoDestinoId()).isEqualTo(40L);
        assertThat(respuesta.getActividadErrorId()).isNull();
    }

    @Test
    void entradaMensajeNoSobrescribeIdentidadVersionEstadoNiRelaciones() {
        Evento nodo = new Evento(); nodo.setId(10L);
        CampoMensaje campo = CampoMensaje.builder().id(20L).build();
        Mensaje mensaje = Mensaje.builder().id(30L).nodo(nodo).correlacionCampo(campo).activo(false).version(4L).build();
        var datos = new MensajeRequestDto(999L, SentidoMensaje.RECEPCION, "Solicitud", true,
                TipoCorrelacion.NEGOCIO, "radicado", 888L, PoliticaSinCorrespondencia.DESCARTAR, 999L);
        mapper.map(datos, mensaje);
        assertThat(mensaje.getId()).isEqualTo(30L);
        assertThat(mensaje.getVersion()).isEqualTo(4L);
        assertThat(mensaje.getActivo()).isFalse();
        assertThat(mensaje.getNodo()).isSameAs(nodo);
        assertThat(mensaje.getCorrelacionCampo()).isSameAs(campo);
        assertThat(mensaje.getNombre()).isEqualTo("Solicitud");
        assertThat(mensaje.getSentido()).isEqualTo(SentidoMensaje.RECEPCION);
        assertThat(mensaje.getOrigenExterno()).isTrue();
        assertThat(mensaje.getCorrelacionTipo()).isEqualTo(TipoCorrelacion.NEGOCIO);
        assertThat(mensaje.getCorrelacionNegocio()).isEqualTo("radicado");
        assertThat(mensaje.getPoliticaSinCorrespondencia()).isEqualTo(PoliticaSinCorrespondencia.DESCARTAR);
    }

    @Test
    void entradaCampoConservaMensajeYControlDeVersion() {
        Mensaje mensaje = Mensaje.builder().id(10L).build();
        CampoMensaje campo = CampoMensaje.builder().id(20L).mensaje(mensaje).activo(false).version(3L).build();
        mapper.map(new CampoMensajeRequestDto("total", TipoDatoMensaje.DECIMAL, 2, 999L), campo);
        assertThat(campo.getId()).isEqualTo(20L);
        assertThat(campo.getMensaje()).isSameAs(mensaje);
        assertThat(campo.getVersion()).isEqualTo(3L);
        assertThat(campo.getActivo()).isFalse();
        assertThat(campo.getNombre()).isEqualTo("total");
        assertThat(campo.getTipoDato()).isEqualTo(TipoDatoMensaje.DECIMAL);
        assertThat(campo.getOrden()).isEqualTo(2);
    }

    @Test
    void entradaFlujoNoConstruyeRelacionesAPartirDeIds() {
        Proceso proceso = Proceso.builder().id(10L).build();
        Pool origen = Pool.builder().id(20L).build(), destino = Pool.builder().id(30L).build();
        Evento nodoOrigen = new Evento(), nodoDestino = new Evento();
        Actividad error = new Actividad();
        FlujoMensaje flujo = FlujoMensaje.builder().id(40L).proceso(proceso).poolOrigen(origen).poolDestino(destino)
                .nodoOrigen(nodoOrigen).nodoDestino(nodoDestino).actividadError(error).activo(false).version(5L).build();
        var datos = new FlujoMensajeRequestDto(999L, 998L, 997L, 996L, TipoDestinoExterno.COLA,
                PoliticaFallo.CONTINUAR, 995L, 999L);
        mapper.map(datos, flujo);
        assertThat(flujo.getId()).isEqualTo(40L);
        assertThat(flujo.getVersion()).isEqualTo(5L);
        assertThat(flujo.getActivo()).isFalse();
        assertThat(flujo.getProceso()).isSameAs(proceso);
        assertThat(flujo.getPoolOrigen()).isSameAs(origen);
        assertThat(flujo.getPoolDestino()).isSameAs(destino);
        assertThat(flujo.getNodoOrigen()).isSameAs(nodoOrigen);
        assertThat(flujo.getNodoDestino()).isSameAs(nodoDestino);
        assertThat(flujo.getActividadError()).isSameAs(error);
        assertThat(flujo.getTipoDestino()).isEqualTo(TipoDestinoExterno.COLA);
        assertThat(flujo.getPoliticaFallo()).isEqualTo(PoliticaFallo.CONTINUAR);

        var nuevo = mapper.map(datos, FlujoMensaje.class);
        assertThat(nuevo.getId()).isNull();
        assertThat(nuevo.getVersion()).isNull();
        assertThat(nuevo.getActivo()).isTrue();
        assertThat(nuevo.getPoolOrigen()).isNull();
        assertThat(nuevo.getNodoOrigen()).isNull();
    }

    @Test
    void entradaPermisoConservaProcesoVersionEIdentidad() {
        Proceso proceso = Proceso.builder().id(10L).build();
        PermisoEstructura permiso = PermisoEstructura.builder().id(20L).proceso(proceso).version(3L).build();
        mapper.map(new PermisoEstructuraRequestDto(RolAcceso.EDITOR, RecursoEstructura.POOL,
                AccionEstructura.CREAR, true, 999L), permiso);
        assertThat(permiso.getId()).isEqualTo(20L);
        assertThat(permiso.getProceso()).isSameAs(proceso);
        assertThat(permiso.getVersion()).isEqualTo(3L);
        assertThat(permiso.getRolAcceso()).isEqualTo(RolAcceso.EDITOR);
        assertThat(permiso.getRecurso()).isEqualTo(RecursoEstructura.POOL);
        assertThat(permiso.getAccion()).isEqualTo(AccionEstructura.CREAR);
        assertThat(permiso.getPermitido()).isTrue();
    }

    @Test
    void nulosPermitenLimpiarOpcionalesEnUnaActualizacionCompleta() {
        Mensaje mensaje = Mensaje.builder().correlacionTipo(TipoCorrelacion.NEGOCIO).correlacionNegocio("radicado")
                .politicaSinCorrespondencia(PoliticaSinCorrespondencia.DESCARTAR).build();
        mapper.map(new MensajeRequestDto(10L, SentidoMensaje.RECEPCION, "Solicitud", false,
                null, null, null, null, 0L), mensaje);
        assertThat(mensaje.getCorrelacionTipo()).isNull();
        assertThat(mensaje.getCorrelacionNegocio()).isNull();
        assertThat(mensaje.getPoliticaSinCorrespondencia()).isNull();
        FlujoMensaje flujo = FlujoMensaje.builder().tipoDestino(TipoDestinoExterno.CORREO)
                .politicaFallo(PoliticaFallo.FINALIZAR).build();
        mapper.map(new FlujoMensajeRequestDto(1L, 2L, 3L, 4L, null, null, null, 0L), flujo);
        assertThat(flujo.getTipoDestino()).isNull();
        assertThat(flujo.getPoliticaFallo()).isNull();
    }
}
