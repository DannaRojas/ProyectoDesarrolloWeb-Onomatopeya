package com.proyecto.inicio.controller;

import com.proyecto.inicio.dto.DiagramaDto.*;
import com.proyecto.inicio.dto.ProcesoDto.Retirada;
import com.proyecto.inicio.service.RolProcesoService;
import com.proyecto.inicio.service.UsuarioSesionService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import java.security.Principal;

@RestController
@RequiredArgsConstructor
@Profile("conexion-empresa")
@RequestMapping("/roles-proceso")
public class RolProcesoController {
    private final RolProcesoService roles;
    private final UsuarioSesionService sesion;

    @GetMapping
    public PaginaRoles buscar(Principal principal, @RequestParam(defaultValue = "") String nombre,
            @RequestParam(defaultValue = "0") @Min(0) int pagina,
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) int tamanio) {
        return roles.buscar(sesion.id(principal), nombre, pagina, tamanio);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RolVista crear(Principal principal, @Valid @RequestBody RolDatos datos) {
        return roles.crear(sesion.id(principal), datos);
    }

    @PutMapping("/{id}")
    public RolVista actualizar(Principal principal, @Positive @PathVariable Long id, @Valid @RequestBody RolDatos datos) {
        return roles.actualizar(sesion.id(principal), id, datos);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void retirar(Principal principal, @Positive @PathVariable Long id, @Valid @RequestBody Retirada datos) {
        roles.retirar(sesion.id(principal), id, datos);
    }
}
