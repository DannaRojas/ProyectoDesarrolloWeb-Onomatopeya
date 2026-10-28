package com.proyecto.inicio.controller;

import com.proyecto.inicio.dto.RegistroEmpresaDto;
import com.proyecto.inicio.dto.request.DatosEmpresa;
import com.proyecto.inicio.dto.response.EmpresaResponseDto;
import com.proyecto.inicio.service.EmpresaService;
import com.proyecto.inicio.service.RegistroEmpresaService;
import com.proyecto.inicio.service.UsuarioSesionService;

import io.swagger.v3.oas.annotations.Operation;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;

import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.security.Principal;
import java.util.List;

@RestController
@Profile("conexion-empresa")
@RequestMapping("/empresas")
public class EmpresaController {
    private final EmpresaService empresaService;
    private final RegistroEmpresaService conexion;
    private final UsuarioSesionService sesion;

    public EmpresaController(EmpresaService empresaService, RegistroEmpresaService conexion,
            UsuarioSesionService sesion) {
        this.empresaService = empresaService;
        this.conexion = conexion;
        this.sesion = sesion;
    }

    @Operation(summary = "Registrar empresa y administrador inicial", description = "Crea la empresa y su primer usuario administrador en una sola transacción. El NIT y el correo del administrador deben ser únicos.")
    @PostMapping
    public ResponseEntity<EmpresaResponseDto> registrar(
            @Valid
            @RequestBody RegistroEmpresaDto registro) {
        EmpresaResponseDto empresa = conexion.registrar(registro);
        URI ubicacion =
                ServletUriComponentsBuilder.fromCurrentRequest()
                        .path("/{id}")
                        .buildAndExpand(empresa.getId())
                        .toUri();
        return ResponseEntity.created(ubicacion).body(empresa);
    }

    @Operation(summary = "Consultar la empresa del usuario", description = "Devuelve únicamente la empresa a la que pertenece el usuario autenticado.")
    @GetMapping
    public List<EmpresaResponseDto> listar(Principal principal) {
        return empresaService.consultarParaUsuario(sesion.id(principal));
    }

    @Operation(summary = "Consultar una empresa propia", description = "Consulta por ID la empresa del usuario autenticado; no permite leer empresas ajenas.")
    @GetMapping("/{id}")
    public EmpresaResponseDto consultar(Principal principal, @Positive @PathVariable Long id) {
        return empresaService.consultarParaUsuario(sesion.id(principal), id);
    }

    @Operation(summary = "Actualizar una empresa propia", description = "Modifica los datos de la empresa propia después de validar el rol de administrador y la unicidad del NIT.")
    @PutMapping("/{id}")
    public EmpresaResponseDto actualizar(
            Principal principal, @Positive
            @PathVariable Long id, @Valid @RequestBody DatosEmpresa datos) {
        return empresaService.actualizar(sesion.id(principal), id, datos.alDtoExistente());
    }
}
