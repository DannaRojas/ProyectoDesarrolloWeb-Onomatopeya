package com.proyecto.inicio.controller.vista;

import com.proyecto.inicio.entity.Empresa;
import com.proyecto.inicio.repository.EmpresaRepository;
import com.proyecto.inicio.repository.UsuarioRepository;
import com.proyecto.inicio.entity.enums.RolAcceso;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import static org.hamcrest.Matchers.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ActiveProfiles("conexion-empresa")
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:pruebas-vistas;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa", "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@Transactional
class EmpresaVistaTest {
    @Autowired WebApplicationContext aplicacion;
    @Autowired EmpresaRepository empresas;
    @Autowired UsuarioRepository usuarios;
    @Autowired PasswordEncoder codificador;
    MockMvc mvc;

    @BeforeEach
    void preparar() {
        mvc = MockMvcBuilders.webAppContextSetup(aplicacion).build();
    }

    @Test
    void muestraListadoVacioConMenuCompartido() throws Exception {
        mvc.perform(get("/vista/empresas")).andExpect(status().isOk())
                .andExpect(view().name("empresas/lista"))
                .andExpect(content().string(containsString("No hay empresas registradas.")))
                .andExpect(content().string(containsString("Gestión de empresas")))
                .andExpect(content().string(not(containsString("Eliminar"))));
    }

    @Test
    void listaYDetalleMuestranDatosEscapados() throws Exception {
        Empresa empresa = empresas.saveAndFlush(Empresa.builder().nombre("<script>alert(1)</script>")
                .nit("900123").correoContacto("contacto@ejemplo.co").activo(true).build());
        mvc.perform(get("/vista/empresas")).andExpect(status().isOk())
                .andExpect(content().string(containsString("&lt;script&gt;")))
                .andExpect(content().string(not(containsString("<script>"))));
        mvc.perform(get("/vista/empresas/" + empresa.getId())).andExpect(status().isOk())
                .andExpect(view().name("empresas/detalle"))
                .andExpect(content().string(containsString("900123")))
                .andExpect(content().string(containsString("contacto@ejemplo.co")));
    }

    @Test
    void erroresDeConsultaDevuelvenUnaPaginaHtml() throws Exception {
        mvc.perform(get("/vista/empresas/999999")).andExpect(status().isNotFound())
                .andExpect(view().name("empresas/error"))
                .andExpect(content().string(containsString("Empresa no encontrada.")));
        mvc.perform(get("/vista/empresas/no-es-id")).andExpect(status().isBadRequest())
                .andExpect(view().name("empresas/error"));
        mvc.perform(get("/vista/empresas/-1")).andExpect(status().isBadRequest());
    }

    @Test
    void apiExistenteConservaJsonYNoHayEliminacionEnLaVista() throws Exception {
        mvc.perform(get("/empresas")).andExpect(status().isOk()).andExpect(content().contentTypeCompatibleWith("application/json"));
        mvc.perform(delete("/vista/empresas/1")).andExpect(status().isMethodNotAllowed());
    }

    @Test
    void muestraRegistroYEdicionConCamposEnlazados() throws Exception {
        mvc.perform(get("/vista/empresas/nueva")).andExpect(status().isOk())
                .andExpect(view().name("empresas/registro"))
                .andExpect(content().string(containsString("Administrador inicial")))
                .andExpect(content().string(containsString("name=\"_token\"")));
        Empresa empresa = crearEmpresa("Empresa existente", "900123");
        mvc.perform(get("/vista/empresas/" + empresa.getId() + "/editar")).andExpect(status().isOk())
                .andExpect(view().name("empresas/editar"))
                .andExpect(content().string(containsString("value=\"Empresa existente\"")))
                .andExpect(content().string(not(containsString("name=\"contrasenaAdmin\""))));
    }

    @Test
    void formularioRegistraEmpresaYAdministradorYRedirigeAlDetalle() throws Exception {
        Formulario sesion = abrirFormulario();
        var resultado = mvc.perform(registro(sesion, "900001", "admin@ejemplo.co"))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("mensaje", "Empresa registrada.")).andReturn();
        var empresa = empresas.findByNit("900001").orElseThrow();
        assertThat(resultado.getResponse().getRedirectedUrl()).isEqualTo("/vista/empresas/" + empresa.getId());
        var admin = usuarios.findByCorreo("admin@ejemplo.co").orElseThrow();
        assertThat(admin.getEmpresa().getId()).isEqualTo(empresa.getId());
        assertThat(admin.getRolAcceso()).isEqualTo(RolAcceso.ADMINISTRADOR);
        assertThat(codificador.matches("ClaveDePrueba123!", admin.getPasswordHash())).isTrue();
        mvc.perform(get(resultado.getResponse().getRedirectedUrl()).flashAttrs(resultado.getFlashMap()))
                .andExpect(status().isOk()).andExpect(content().string(containsString("Empresa registrada.")))
                .andExpect(content().string(not(containsString("ClaveDePrueba123!"))));
        assertThat(empresas.count()).isEqualTo(1);
    }

    @Test
    void datosInvalidosConservanCamposPeroNoLaContrasena() throws Exception {
        var peticion = registro(abrirFormulario(), "900002", "admin@ejemplo.co").with(request -> {
            request.setParameter("correoContacto", "incorrecto"); return request;
        });
        mvc.perform(peticion).andExpect(status().isBadRequest())
                .andExpect(view().name("empresas/registro"))
                .andExpect(model().attributeHasFieldErrors("registro", "correoContacto"))
                .andExpect(content().string(containsString("Escribe un correo válido.")))
                .andExpect(content().string(containsString("value=\"Empresa de prueba\"")))
                .andExpect(content().string(not(containsString("ClaveDePrueba123!"))));
        assertThat(empresas.count()).isZero();
        assertThat(usuarios.count()).isZero();
    }

    @Test
    void nitRepetidoMuestraErrorSinGuardarOtroAdministrador() throws Exception {
        crearEmpresa("Ya existe", "900003");
        mvc.perform(registro(abrirFormulario(), "900003", "otro@ejemplo.co"))
                .andExpect(status().isConflict())
                .andExpect(model().attributeHasFieldErrors("registro", "nit"))
                .andExpect(content().string(containsString("El NIT ya está registrado.")))
                .andExpect(content().string(not(containsString("ClaveDePrueba123!"))));
        assertThat(empresas.count()).isEqualTo(1);
        assertThat(usuarios.count()).isZero();
    }

    @Test
    void correoAdministradorRepetidoNoDejaEmpresaParcial() throws Exception {
        Formulario sesion = abrirFormulario();
        mvc.perform(registro(sesion, "900004", "admin@ejemplo.co")).andExpect(status().is3xxRedirection());
        mvc.perform(registro(sesion, "900005", "admin@ejemplo.co")).andExpect(status().isConflict())
                .andExpect(model().attributeHasFieldErrors("registro", "correoAdmin"));
        assertThat(empresas.findByNit("900005")).isEmpty();
        assertThat(usuarios.count()).isEqualTo(1);
    }

    @Test
    void editaEmpresaSinPermitirCambiosDeIdOEstado() throws Exception {
        Empresa empresa = crearEmpresa("Antes", "900006");
        mvc.perform(edicion(abrirFormulario(), empresa.getId(), "Después", "900007")
                .param("id", "99999").param("activo", "false"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/vista/empresas/" + empresa.getId()))
                .andExpect(flash().attribute("mensaje", "Cambios guardados."));
        assertThat(empresas.count()).isEqualTo(1);
        var guardada = empresas.findById(empresa.getId()).orElseThrow();
        assertThat(guardada.getNombre()).isEqualTo("Después");
        assertThat(guardada.getNit()).isEqualTo("900007");
        assertThat(guardada.getActivo()).isTrue();
    }

    @Test
    void errorDeEdicionMantieneElFormularioYNoCambiaLosDatos() throws Exception {
        Empresa empresa = crearEmpresa("Antes", "900008");
        crearEmpresa("Otra", "900009");
        mvc.perform(edicion(abrirFormulario(), empresa.getId(), "Intento", "900009"))
                .andExpect(status().isConflict()).andExpect(view().name("empresas/editar"))
                .andExpect(model().attributeHasFieldErrors("formulario", "nit"))
                .andExpect(content().string(containsString("value=\"Intento\"")));
        assertThat(empresas.findById(empresa.getId()).orElseThrow().getNombre()).isEqualTo("Antes");
    }

    @Test
    void rechazaFormulariosSinTokenODeOtraSesion() throws Exception {
        Formulario primera = abrirFormulario(), segunda = abrirFormulario();
        mvc.perform(registro(primera, "900010", "admin@ejemplo.co").with(request -> {
            request.removeParameter("_token"); return request;
        })).andExpect(status().isForbidden()).andExpect(view().name("empresas/error"));
        mvc.perform(registro(segunda, "900010", "admin@ejemplo.co").with(request -> {
            request.setParameter("_token", primera.token()); return request;
        })).andExpect(status().isForbidden());
        assertThat(empresas.count()).isZero();
    }

    @Test
    void edicionInvalidaONoExistenteNoGuardaDatos() throws Exception {
        Empresa empresa = crearEmpresa("Antes", "900011");
        mvc.perform(edicion(abrirFormulario(), empresa.getId(), " ", "900011"))
                .andExpect(status().isBadRequest())
                .andExpect(model().attributeHasFieldErrors("formulario", "nombre"));
        mvc.perform(edicion(abrirFormulario(), 999999L, "No existe", "900012"))
                .andExpect(status().isNotFound()).andExpect(view().name("empresas/error"));
        assertThat(empresas.count()).isEqualTo(1);
    }

    private Empresa crearEmpresa(String nombre, String nit) {
        return empresas.saveAndFlush(Empresa.builder().nombre(nombre).nit(nit)
                .correoContacto("contacto@ejemplo.co").activo(true).build());
    }

    private Formulario abrirFormulario() throws Exception {
        var resultado = mvc.perform(get("/vista/empresas/nueva")).andExpect(status().isOk()).andReturn();
        return new Formulario((MockHttpSession) resultado.getRequest().getSession(false),
                (String) resultado.getModelAndView().getModel().get("tokenFormulario"));
    }

    private MockHttpServletRequestBuilder registro(Formulario formulario, String nit, String correoAdmin) {
        return post("/vista/empresas").session(formulario.sesion()).param("_token", formulario.token())
                .param("nombre", "Empresa de prueba").param("nit", nit).param("correoContacto", "contacto@ejemplo.co")
                .param("nombreAdmin", "Administrador").param("correoAdmin", correoAdmin)
                .param("contrasenaAdmin", "ClaveDePrueba123!");
    }

    private MockHttpServletRequestBuilder edicion(Formulario formulario, Long id, String nombre, String nit) {
        return post("/vista/empresas/" + id + "/editar").session(formulario.sesion()).param("_token", formulario.token())
                .param("nombre", nombre).param("nit", nit).param("correoContacto", "contacto@ejemplo.co");
    }

    private record Formulario(MockHttpSession sesion, String token) {}
}
