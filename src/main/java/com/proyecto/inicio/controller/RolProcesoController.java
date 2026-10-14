package com.proyecto.inicio.controller;

import com.proyecto.inicio.dto.request.Retirada;
import com.proyecto.inicio.dto.request.RolDatos;
import com.proyecto.inicio.dto.response.PaginaRoles;
import com.proyecto.inicio.dto.response.RolVista;
import com.proyecto.inicio.service.RolProcesoService;
import com.proyecto.inicio.service.UsuarioSesionService;

import io.swagger.v3.oas.annotations.Operation;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;

import lombok.RequiredArgsConstructor;

import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;

@RestController
@RequiredArgsConstructor
@Profile("conexion-empresa")
@RequestMapping("/roles-proceso")
public class RolProcesoController {
    private final RolProcesoService roles;
    private final UsuarioSesionService sesion;

    @Operation(summary = "Buscar roles de proceso")
    @GetMapping
    public PaginaRoles buscar(
            Principal principal,
            @RequestParam(defaultValue = "") String nombre,
            @RequestParam(defaultValue = "0")
            @Min(0) int pagina,
            @RequestParam(defaultValue = "10")
            @Min(1)
            @Max(100) int tamanio) {
        return roles.buscar(sesion.id(principal), nombre, pagina, tamanio);
    }

    @Operation(summary = "Crear un rol de proceso")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RolVista crear(Principal principal, @Valid @RequestBody RolDatos datos) {
        return roles.crear(sesion.id(principal), datos);
    }

    @Operation(summary = "Actualizar un rol de proceso")
    @PutMapping("/{id}")
    public RolVista actualizar(
            Principal principal,
            @Positive
            @PathVariable Long id,
            @Valid
            @RequestBody RolDatos datos) {
        return roles.actualizar(sesion.id(principal), id, datos);
    }

    @Operation(summary = "Desactivar un rol de proceso")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void retirar(
            Principal principal,
            @Positive
            @PathVariable Long id,
            @Valid
            @RequestBody Retirada datos) {
        roles.retirar(sesion.id(principal), id, datos);
    }
}
