package com.proyecto.inicio.controller;

import com.proyecto.inicio.dto.ProcesoDto.*;
import com.proyecto.inicio.entity.enums.EstadoPublicacion;
import com.proyecto.inicio.service.ProcesoService;
import com.proyecto.inicio.service.UsuarioSesionService;
import com.proyecto.inicio.service.DiagramaService;
import com.proyecto.inicio.dto.DiagramaDto.Detalle;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import java.security.Principal;
import java.util.List;

@RestController
@RequiredArgsConstructor
@Profile("conexion-empresa")
@RequestMapping("/procesos")
public class ProcesoController {
    private final ProcesoService procesos;
    private final UsuarioSesionService sesion;
    private final DiagramaService diagramas;

    @GetMapping("/{id}/diagrama")
    public Detalle diagrama(Principal principal, @Positive @PathVariable Long id) {
        return diagramas.consultar(sesion.id(principal), id);
    }

    @GetMapping
    public Pagina buscar(Principal principal, @RequestParam(defaultValue = "") String nombre,
            @RequestParam(defaultValue = "") String categoria,
            @RequestParam(required = false) EstadoPublicacion estado,
            @RequestParam(defaultValue = "true") boolean activo,
            @RequestParam(defaultValue = "0") @Min(0) int pagina,
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) int tamanio) {
        return procesos.buscar(sesion.id(principal), nombre, categoria, estado, activo, pagina, tamanio);
    }

    @GetMapping("/{id}")
    public Ficha consultar(Principal principal, @Positive @PathVariable Long id) {
        return procesos.consultar(sesion.id(principal), id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Ficha crear(Principal principal, @Valid @RequestBody Datos datos) {
        return procesos.crear(sesion.id(principal), datos);
    }

    @PutMapping("/{id}")
    public Ficha actualizar(Principal principal, @Positive @PathVariable Long id, @Valid @RequestBody Datos datos) {
        return procesos.actualizar(sesion.id(principal), id, datos);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void retirar(Principal principal, @Positive @PathVariable Long id, @Valid @RequestBody Retirada datos) {
        procesos.retirar(sesion.id(principal), id, datos);
    }

    @GetMapping("/{id}/historial")
    public List<Cambio> historial(Principal principal, @Positive @PathVariable Long id) {
        return procesos.historial(sesion.id(principal), id);
    }

    @PutMapping("/{id}/comparticion")
    public Acceso compartir(Principal principal, @Positive @PathVariable Long id, @Valid @RequestBody Compartir datos) {
        return procesos.compartir(sesion.id(principal), id, datos);
    }
}
