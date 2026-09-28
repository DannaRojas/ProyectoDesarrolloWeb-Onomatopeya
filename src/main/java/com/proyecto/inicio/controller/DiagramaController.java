package com.proyecto.inicio.controller;

import com.proyecto.inicio.dto.DiagramaDto.*;
import com.proyecto.inicio.dto.ProcesoDto.Retirada;
import com.proyecto.inicio.service.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
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

    @GetMapping("/pools")
    public List<PoolVista> pools(Principal actor, @Positive @PathVariable Long procesoId) {
        return pools.listar(sesion.id(actor), procesoId);
    }

    @PostMapping("/pools")
    @ResponseStatus(HttpStatus.CREATED)
    public PoolVista crearPool(Principal actor, @Positive @PathVariable Long procesoId, @Valid @RequestBody PoolDatos datos) {
        return pools.crear(sesion.id(actor), procesoId, datos);
    }

    @PutMapping("/pools/{id}")
    public PoolVista editarPool(Principal actor, @Positive @PathVariable Long procesoId,
            @Positive @PathVariable Long id, @Valid @RequestBody PoolDatos datos) {
        return pools.actualizar(sesion.id(actor), procesoId, id, datos);
    }

    @PutMapping("/pools/orden")
    public List<PoolVista> reordenarPools(Principal actor, @Positive @PathVariable Long procesoId,
            @Valid @RequestBody ReordenarPools datos) {
        return pools.reordenar(sesion.id(actor), procesoId, datos);
    }

    @DeleteMapping("/pools/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void retirarPool(Principal actor, @Positive @PathVariable Long procesoId,
            @Positive @PathVariable Long id, @Valid @RequestBody Retirada datos) {
        pools.retirar(sesion.id(actor), procesoId, id, datos);
    }

    @GetMapping("/pools/{poolId}/lanes")
    public List<LaneVista> lanes(Principal actor, @Positive @PathVariable Long procesoId,
            @Positive @PathVariable Long poolId) {
        return lanes.listar(sesion.id(actor), procesoId, poolId);
    }

    @PostMapping("/pools/{poolId}/lanes")
    @ResponseStatus(HttpStatus.CREATED)
    public LaneVista crearLane(Principal actor, @Positive @PathVariable Long procesoId,
            @Positive @PathVariable Long poolId, @Valid @RequestBody LaneDatos datos) {
        return lanes.crear(sesion.id(actor), procesoId, poolId, datos);
    }

    @PutMapping("/pools/{poolId}/lanes/{id}")
    public LaneVista editarLane(Principal actor, @Positive @PathVariable Long procesoId,
            @Positive @PathVariable Long poolId, @Positive @PathVariable Long id, @Valid @RequestBody LaneDatos datos) {
        return lanes.actualizar(sesion.id(actor), procesoId, poolId, id, datos);
    }

    @DeleteMapping("/pools/{poolId}/lanes/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void retirarLane(Principal actor, @Positive @PathVariable Long procesoId,
            @Positive @PathVariable Long poolId, @Positive @PathVariable Long id, @Valid @RequestBody Retirada datos) {
        lanes.retirar(sesion.id(actor), procesoId, poolId, id, datos);
    }

    @PutMapping("/pools/{poolId}/lanes/orden")
    public List<LaneVista> reordenar(Principal actor, @Positive @PathVariable Long procesoId,
            @Positive @PathVariable Long poolId, @Valid @RequestBody Reordenar datos) {
        return lanes.reordenar(sesion.id(actor), procesoId, poolId, datos);
    }

    @GetMapping("/nodos")
    public List<NodoVista> nodos(Principal actor, @Positive @PathVariable Long procesoId) {
        return nodos.listar(sesion.id(actor), procesoId);
    }

    @PostMapping("/nodos")
    @ResponseStatus(HttpStatus.CREATED)
    public NodoVista crearNodo(Principal actor, @Positive @PathVariable Long procesoId, @Valid @RequestBody NodoDatos datos) {
        return nodos.crear(sesion.id(actor), procesoId, datos);
    }

    @PutMapping("/nodos/{id}")
    public NodoVista editarNodo(Principal actor, @Positive @PathVariable Long procesoId,
            @Positive @PathVariable Long id, @Valid @RequestBody NodoDatos datos) {
        return nodos.actualizar(sesion.id(actor), procesoId, id, datos);
    }

    @DeleteMapping("/nodos/{id}")
    public List<String> retirarNodo(Principal actor, @Positive @PathVariable Long procesoId,
            @Positive @PathVariable Long id, @Valid @RequestBody Retirada datos) {
        return nodos.retirar(sesion.id(actor), procesoId, id, datos);
    }

    @GetMapping("/arcos")
    public List<ArcoVista> arcos(Principal actor, @Positive @PathVariable Long procesoId) {
        return arcos.listar(sesion.id(actor), procesoId);
    }

    @PostMapping("/arcos")
    @ResponseStatus(HttpStatus.CREATED)
    public ArcoVista crearArco(Principal actor, @Positive @PathVariable Long procesoId, @Valid @RequestBody ArcoDatos datos) {
        return arcos.crear(sesion.id(actor), procesoId, datos);
    }

    @PutMapping("/arcos/{id}")
    public ArcoVista editarArco(Principal actor, @Positive @PathVariable Long procesoId,
            @Positive @PathVariable Long id, @Valid @RequestBody ArcoDatos datos) {
        return arcos.actualizar(sesion.id(actor), procesoId, id, datos);
    }

    @DeleteMapping("/arcos/{id}")
    public List<String> retirarArco(Principal actor, @Positive @PathVariable Long procesoId,
            @Positive @PathVariable Long id, @Valid @RequestBody Retirada datos) {
        return arcos.retirar(sesion.id(actor), procesoId, id, datos);
    }
}
