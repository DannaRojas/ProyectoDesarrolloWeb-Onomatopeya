package com.proyecto.inicio.service;

import com.proyecto.inicio.dto.response.Detalle;
import com.proyecto.inicio.dto.response.Ficha;
import com.proyecto.inicio.dto.response.LaneVista;
import com.proyecto.inicio.dto.response.MensajeDetalle;
import com.proyecto.inicio.dto.response.PoolVista;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.util.ArrayList;
import java.util.List;

@Service
@Validated
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DiagramaService {
    private final ProcesoService procesos;
    private final PoolService pools;
    private final LaneService lanes;
    private final NodoService nodos;
    private final ArcoService arcos;
    private final MensajeService mensajes;
    private final FlujoMensajeService flujos;

    public Detalle consultar(@NotNull @Positive Long actorId, @NotNull @Positive Long procesoId) {
        Ficha proceso = procesos.consultar(actorId, procesoId);
        List<PoolVista> listaPools = pools.listar(actorId, procesoId);
        List<LaneVista> listaLanes =
                listaPools.stream()
                        .flatMap(p -> lanes.listar(actorId, procesoId, p.id()).stream())
                        .toList();
        List<MensajeDetalle> listaMensajes = mensajes.consultarDetalles(actorId, procesoId);
        List<String> avisos = new ArrayList<>(nodos.advertencias(actorId, procesoId));
        if (Boolean.TRUE.equals(proceso.activo()))
            avisos.addAll(mensajes.advertencias(actorId, procesoId));
        return new Detalle(
                proceso,
                listaPools,
                listaLanes,
                nodos.listar(actorId, procesoId),
                arcos.listar(actorId, procesoId),
                listaMensajes,
                flujos.listarParaDiagrama(actorId, procesoId),
                avisos);
    }
}
