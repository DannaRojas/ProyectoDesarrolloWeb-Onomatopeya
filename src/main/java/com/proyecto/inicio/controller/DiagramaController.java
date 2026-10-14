package com.proyecto.inicio.controller;

import com.proyecto.inicio.dto.request.ArcoDatos;
import com.proyecto.inicio.dto.request.LaneDatos;
import com.proyecto.inicio.dto.request.NodoDatos;
import com.proyecto.inicio.dto.request.PoolDatos;
import com.proyecto.inicio.dto.request.Reordenar;
import com.proyecto.inicio.dto.request.ReordenarPools;
import com.proyecto.inicio.dto.request.Retirada;
import com.proyecto.inicio.dto.response.ArcoVista;
import com.proyecto.inicio.dto.response.LaneVista;
import com.proyecto.inicio.dto.response.NodoVista;
import com.proyecto.inicio.dto.response.PoolVista;
import com.proyecto.inicio.service.ArcoService;
import com.proyecto.inicio.service.LaneService;
import com.proyecto.inicio.service.NodoService;
import com.proyecto.inicio.service.PoolService;
import com.proyecto.inicio.service.UsuarioSesionService;

import io.swagger.v3.oas.annotations.Operation;

import jakarta.validation.Valid;
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
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.List;

@RestController
@RequiredArgsConstructor
@Profile("conexion-empresa")
@RequestMapping("/procesos/{procesoId}")
public class DiagramaController {
    private final PoolService pools;
    private final LaneService lanes;
    private final NodoService nodos;
    private final ArcoService arcos;
    private final UsuarioSesionService sesion;

    @Operation(summary = "Listar pools del proceso")
    @GetMapping("/pools")
    public List<PoolVista> pools(Principal actor, @Positive @PathVariable Long procesoId) {
        return pools.listar(sesion.id(actor), procesoId);
    }

    @Operation(summary = "Crear un pool externo")
    @PostMapping("/pools")
    @ResponseStatus(HttpStatus.CREATED)
    public PoolVista crearPool(
            Principal actor,
            @Positive
            @PathVariable Long procesoId,
            @Valid
            @RequestBody PoolDatos datos) {
        return pools.crear(sesion.id(actor), procesoId, datos);
    }

    @Operation(summary = "Actualizar un pool")
    @PutMapping("/pools/{id}")
    public PoolVista editarPool(
            Principal actor,
            @Positive
            @PathVariable Long procesoId,
            @Positive
            @PathVariable Long id,
            @Valid
            @RequestBody PoolDatos datos) {
        return pools.actualizar(sesion.id(actor), procesoId, id, datos);
    }

    @Operation(summary = "Reordenar los pools")
    @PutMapping("/pools/orden")
    public List<PoolVista> reordenarPools(
            Principal actor,
            @Positive
            @PathVariable Long procesoId,
            @Valid
            @RequestBody ReordenarPools datos) {
        return pools.reordenar(sesion.id(actor), procesoId, datos);
    }

    @Operation(summary = "Retirar un pool")
    @DeleteMapping("/pools/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void retirarPool(
            Principal actor,
            @Positive
            @PathVariable Long procesoId,
            @Positive
            @PathVariable Long id,
            @Valid
            @RequestBody Retirada datos) {
        pools.retirar(sesion.id(actor), procesoId, id, datos);
    }

    @Operation(summary = "Listar lanes de un pool")
    @GetMapping("/pools/{poolId}/lanes")
    public List<LaneVista> lanes(
            Principal actor,
            @Positive
            @PathVariable Long procesoId,
            @Positive
            @PathVariable Long poolId) {
        return lanes.listar(sesion.id(actor), procesoId, poolId);
    }

    @Operation(summary = "Crear una lane")
    @PostMapping("/pools/{poolId}/lanes")
    @ResponseStatus(HttpStatus.CREATED)
    public LaneVista crearLane(
            Principal actor,
            @Positive
            @PathVariable Long procesoId,
            @Positive
            @PathVariable Long poolId,
            @Valid
            @RequestBody LaneDatos datos) {
        return lanes.crear(sesion.id(actor), procesoId, poolId, datos);
    }

    @Operation(summary = "Actualizar una lane")
    @PutMapping("/pools/{poolId}/lanes/{id}")
    public LaneVista editarLane(
            Principal actor,
            @Positive
            @PathVariable Long procesoId,
            @Positive
            @PathVariable Long poolId,
            @Positive
            @PathVariable Long id,
            @Valid
            @RequestBody LaneDatos datos) {
        return lanes.actualizar(sesion.id(actor), procesoId, poolId, id, datos);
    }

    @Operation(summary = "Retirar una lane")
    @DeleteMapping("/pools/{poolId}/lanes/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void retirarLane(
            Principal actor,
            @Positive
            @PathVariable Long procesoId,
            @Positive
            @PathVariable Long poolId,
            @Positive
            @PathVariable Long id,
            @Valid
            @RequestBody Retirada datos) {
        lanes.retirar(sesion.id(actor), procesoId, poolId, id, datos);
    }

    @Operation(summary = "Reordenar las lanes")
    @PutMapping("/pools/{poolId}/lanes/orden")
    public List<LaneVista> reordenar(
            Principal actor,
            @Positive
            @PathVariable Long procesoId,
            @Positive
            @PathVariable Long poolId,
            @Valid
            @RequestBody Reordenar datos) {
        return lanes.reordenar(sesion.id(actor), procesoId, poolId, datos);
    }

    @Operation(summary = "Listar nodos del proceso")
    @GetMapping("/nodos")
    public List<NodoVista> nodos(Principal actor, @Positive @PathVariable Long procesoId) {
        return nodos.listar(sesion.id(actor), procesoId);
    }

    @Operation(summary = "Crear un nodo")
    @PostMapping("/nodos")
    @ResponseStatus(HttpStatus.CREATED)
    public NodoVista crearNodo(
            Principal actor,
            @Positive
            @PathVariable Long procesoId,
            @Valid
            @RequestBody NodoDatos datos) {
        return nodos.crear(sesion.id(actor), procesoId, datos);
    }

    @Operation(summary = "Actualizar un nodo")
    @PutMapping("/nodos/{id}")
    public NodoVista editarNodo(
            Principal actor,
            @Positive
            @PathVariable Long procesoId,
            @Positive
            @PathVariable Long id,
            @Valid
            @RequestBody NodoDatos datos) {
        return nodos.actualizar(sesion.id(actor), procesoId, id, datos);
    }

    @Operation(summary = "Retirar un nodo")
    @DeleteMapping("/nodos/{id}")
    public List<String> retirarNodo(
            Principal actor,
            @Positive
            @PathVariable Long procesoId,
            @Positive
            @PathVariable Long id,
            @Valid
            @RequestBody Retirada datos) {
        return nodos.retirar(sesion.id(actor), procesoId, id, datos);
    }

    @Operation(summary = "Listar arcos del proceso")
    @GetMapping("/arcos")
    public List<ArcoVista> arcos(Principal actor, @Positive @PathVariable Long procesoId) {
        return arcos.listar(sesion.id(actor), procesoId);
    }

    @Operation(summary = "Crear un arco")
    @PostMapping("/arcos")
    @ResponseStatus(HttpStatus.CREATED)
    public ArcoVista crearArco(
            Principal actor,
            @Positive
            @PathVariable Long procesoId,
            @Valid
            @RequestBody ArcoDatos datos) {
        return arcos.crear(sesion.id(actor), procesoId, datos);
    }

    @Operation(summary = "Actualizar un arco")
    @PutMapping("/arcos/{id}")
    public ArcoVista editarArco(
            Principal actor,
            @Positive
            @PathVariable Long procesoId,
            @Positive
            @PathVariable Long id,
            @Valid
            @RequestBody ArcoDatos datos) {
        return arcos.actualizar(sesion.id(actor), procesoId, id, datos);
    }

    @Operation(summary = "Retirar un arco")
    @DeleteMapping("/arcos/{id}")
    public List<String> retirarArco(
            Principal actor,
            @Positive
            @PathVariable Long procesoId,
            @Positive
            @PathVariable Long id,
            @Valid
            @RequestBody Retirada datos) {
        return arcos.retirar(sesion.id(actor), procesoId, id, datos);
    }
}
