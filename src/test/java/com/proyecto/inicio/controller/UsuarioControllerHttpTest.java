package com.proyecto.inicio.controller;

import com.proyecto.inicio.dto.request.InvitarUsuarioRequestDto;
import com.proyecto.inicio.entity.Empresa;
import com.proyecto.inicio.entity.Usuario;
import com.proyecto.inicio.entity.enums.EstadoUsuario;
import com.proyecto.inicio.entity.enums.RolAcceso;
import com.proyecto.inicio.repository.EmpresaRepository;
import com.proyecto.inicio.repository.UsuarioRepository;
import com.proyecto.inicio.service.UsuarioInvitacionService;
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

import java.util.Map;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ActiveProfiles("conexion-empresa")
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:pruebas-http-usuarios;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa", "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.properties.hibernate.hbm2ddl.halt_on_error=true"
})
@Transactional
class UsuarioControllerHttpTest {
    @Autowired WebApplicationContext aplicacion;
    @Autowired EmpresaRepository empresas;
    @Autowired UsuarioRepository usuarios;
    @Autowired UsuarioInvitacionService invitaciones;
    MockMvc mvc;
    final ObjectMapper json = new ObjectMapper();

    @BeforeEach
    void preparar() {
        mvc = MockMvcBuilders.webAppContextSetup(aplicacion).build();
    }

    @Test
    void aceptaPorHttpSinExponerCredencialesYRechazaReutilizacion() throws Exception {
        Empresa empresa = empresas.save(Empresa.builder().nombre("Empresa HTTP")
                .nit(UUID.randomUUID().toString()).correoContacto("contacto@example.com").activo(true).build());
        Usuario admin = usuarios.save(Usuario.builder().empresa(empresa).nombre("Administrador")
                .correo(UUID.randomUUID() + "@example.com").estado(EstadoUsuario.ACTIVO)
                .rolAcceso(RolAcceso.ADMINISTRADOR).build());
        var datos = new InvitarUsuarioRequestDto();
        datos.setNombre("Persona invitada");
        datos.setCorreo(UUID.randomUUID() + "@example.com");
        datos.setRolAcceso(RolAcceso.EDITOR);
        String token = invitaciones.invitar(admin.getId(), datos).getTokenInvitacion();
        String cuerpo = json.writeValueAsString(Map.of("token", token, "contrasena", "ClaveDePrueba123!"));
        mvc.perform(post("/usuarios/invitaciones/aceptar").contentType("application/json").content(cuerpo))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("ACTIVO"))
                .andExpect(jsonPath("$.empresaId").value(empresa.getId()))
                .andExpect(jsonPath("$.passwordHash").doesNotExist())
                .andExpect(jsonPath("$.tokenInvitacionHash").doesNotExist())
                .andExpect(jsonPath("$.contrasena").doesNotExist());
        mvc.perform(post("/usuarios/invitaciones/aceptar").contentType("application/json").content(cuerpo))
                .andExpect(status().isBadRequest());
    }

    @Test
    void validaCuerpoYDevuelveErroresControlados() throws Exception {
        mvc.perform(post("/usuarios/invitaciones/aceptar").contentType("application/json")
                        .content("{\"token\":\"incorrecto\",\"contrasena\":\"corta\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.token").exists())
                .andExpect(jsonPath("$.campos.contrasena").exists());
        mvc.perform(post("/usuarios/invitaciones/aceptar").contentType("application/json")
                        .content(json.writeValueAsString(Map.of("token", "a".repeat(43), "contrasena", "ClaveDePrueba123!"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("La invitación no es válida o ya venció."));
    }
}
