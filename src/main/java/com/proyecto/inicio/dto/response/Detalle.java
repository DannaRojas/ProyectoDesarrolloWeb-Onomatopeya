package com.proyecto.inicio.dto.response;

import java.util.List;

public record Detalle(
        Ficha proceso,
        List<PoolVista> pools,
        List<LaneVista> lanes,
        List<NodoVista> nodos,
        List<ArcoVista> arcos,
        List<MensajeDetalle> mensajes,
        List<FlujoMensajeResponseDto> flujos,
        List<String> advertencias) {}
