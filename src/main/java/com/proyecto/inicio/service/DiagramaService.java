package com.proyecto.inicio.service;

import com.proyecto.inicio.dto.DiagramaDto.*;
import com.proyecto.inicio.dto.MensajeDto.*;
import com.proyecto.inicio.dto.FlujoMensajeDto.FlujoMensajeResponseDto;
import com.proyecto.inicio.repository.*;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import jakarta.validation.constraints.*;
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
    private final MensajeRepository declaraciones;
    private final CampoMensajeRepository campos;
    private final UsoMensajeActividadRepository usos;
    private final FlujoMensajeRepository flujos;
    private final ModelMapper mapper;

    public Detalle consultar(@NotNull @Positive Long actorId, @NotNull @Positive Long procesoId) {
        var proceso = procesos.consultar(actorId, procesoId);
        var listaPools = pools.listar(actorId, procesoId);
        var listaLanes = listaPools.stream().flatMap(p -> lanes.listar(actorId, procesoId, p.id()).stream()).toList();
        var listaMensajes = declaraciones.listarActivosPorProceso(procesoId).stream()
                .map(m -> new MensajeDetalle(mapper.map(m, MensajeResponseDto.class),
                        campos.listarActivos(m.getId()).stream().map(c -> mapper.map(c, CampoMensajeResponseDto.class)).toList(),
                        usos.listarActivos(m.getId()).stream().map(u -> mapper.map(u, UsoMensajeActividadResponseDto.class)).toList())).toList();
        List<String> avisos = new ArrayList<>(nodos.advertencias(actorId, procesoId));
        if (Boolean.TRUE.equals(proceso.activo())) avisos.addAll(mensajes.advertencias(actorId, procesoId));
        return new Detalle(proceso, listaPools, listaLanes, nodos.listar(actorId, procesoId),
                arcos.listar(actorId, procesoId), listaMensajes,
                flujos.listarActivos(procesoId).stream().map(f -> mapper.map(f, FlujoMensajeResponseDto.class)).toList(), avisos);
    }
}
