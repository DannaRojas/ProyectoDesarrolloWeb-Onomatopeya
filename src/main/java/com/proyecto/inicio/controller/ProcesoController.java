package com.proyecto.inicio.controller;

import com.proyecto.inicio.dto.request.Compartir;
import com.proyecto.inicio.dto.request.Datos;
import com.proyecto.inicio.dto.request.Retirada;
import com.proyecto.inicio.dto.response.Acceso;
import com.proyecto.inicio.dto.response.Cambio;
import com.proyecto.inicio.dto.response.Detalle;
import com.proyecto.inicio.dto.response.Ficha;
import com.proyecto.inicio.dto.response.Pagina;
import com.proyecto.inicio.entity.enums.EstadoPublicacion;
import com.proyecto.inicio.service.DiagramaService;
import com.proyecto.inicio.service.ProcesoService;
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
import java.util.List;

@RestController
@RequiredArgsConstructor
@Profile("conexion-empresa")
@RequestMapping("/procesos")
public class ProcesoController {
    private final ProcesoService procesos;
    private final UsuarioSesionService sesion;
    private final DiagramaService diagramas;

    @Operation(summary = "Consultar el diagrama completo", description = "Reúne los elementos vigentes del diagrama de un proceso accesible para el usuario.")
    @GetMapping("/{id}/diagrama")
    public Detalle diagrama(Principal principal, @Positive @PathVariable Long id) {
        return diagramas.consultar(sesion.id(principal), id);
    }

    @Operation(summary = "Buscar procesos con filtros y paginación", description = "Filtra por nombre, categoría, publicación y estado activo; devuelve una página de procesos visibles para el usuario.")
    @GetMapping
    public Pagina buscar(
            Principal principal,
            @RequestParam(defaultValue = "") String nombre,
            @RequestParam(defaultValue = "") String categoria,
            @RequestParam(required = false) EstadoPublicacion estado,
            @RequestParam(defaultValue = "true") boolean activo,
            @RequestParam(defaultValue = "0")
            @Min(0) int pagina,
            @RequestParam(defaultValue = "10")
            @Min(1)
            @Max(100) int tamanio) {
        return procesos.buscar(
                sesion.id(principal), nombre, categoria, estado, activo, pagina, tamanio);
    }

    @Operation(summary = "Consultar un proceso", description = "Muestra la ficha de un proceso propio o compartido para consulta, sin cambiar su empresa propietaria.")
    @GetMapping("/{id}")
    public Ficha consultar(Principal principal, @Positive @PathVariable Long id) {
        return procesos.consultar(sesion.id(principal), id);
    }

    @Operation(summary = "Crear un proceso en borrador", description = "Crea un proceso de la empresa actual y su pool propietario; el diagrama puede completarse después.")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Ficha crear(Principal principal, @Valid @RequestBody Datos datos) {
        return procesos.crear(sesion.id(principal), datos);
    }

    @Operation(summary = "Actualizar un proceso", description = "Edita los datos del proceso tras validar acceso, nombre único dentro de la empresa y versión vigente.")
    @PutMapping("/{id}")
    public Ficha actualizar(
            Principal principal, @Positive @PathVariable Long id, @Valid @RequestBody Datos datos) {
        return procesos.actualizar(sesion.id(principal), id, datos);
    }

    @Operation(summary = "Desactivar un proceso", description = "Retira lógicamente el proceso con confirmación; conserva los registros e historial asociados.")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void retirar(
            Principal principal,
            @Positive
            @PathVariable Long id,
            @Valid
            @RequestBody Retirada datos) {
        procesos.retirar(sesion.id(principal), id, datos);
    }

    @Operation(summary = "Consultar el historial de un proceso", description = "Lista los cambios registrados para un proceso que el usuario puede consultar.")
    @GetMapping("/{id}/historial")
    public List<Cambio> historial(Principal principal, @Positive @PathVariable Long id) {
        return procesos.historial(sesion.id(principal), id);
    }

    @Operation(summary = "Compartir un proceso para consulta", description = "Concede a otra empresa acceso de solo lectura, sin transferir la propiedad del proceso.")
    @PutMapping("/{id}/comparticion")
    public Acceso compartir(
            Principal principal,
            @Positive
            @PathVariable Long id,
            @Valid
            @RequestBody Compartir datos) {
        return procesos.compartir(sesion.id(principal), id, datos);
    }
}
