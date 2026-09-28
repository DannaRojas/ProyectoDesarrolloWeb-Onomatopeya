package com.proyecto.inicio.dto;

import com.proyecto.inicio.entity.enums.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.List;

public final class DiagramaDto {
    private DiagramaDto() {
    }

    public record RolDatos(@NotBlank @Size(max = 150) String nombre, @Size(max = 10000) String descripcion,
            @PositiveOrZero Long version) {
    }

    public record RolVista(Long id, String nombre, String descripcion, Boolean activo, Long version,
            List<Long> procesosEnUso) {
    }

    public record PaginaRoles(List<RolVista> contenido, int pagina, int tamanio, long total, int paginas) {
    }

    public record PoolDatos(@NotBlank @Size(max = 150) String nombre, @NotNull TipoParticipante tipoParticipante,
            @NotNull Boolean cajaNegra, @NotNull @PositiveOrZero Integer posicionX,
            @NotNull @PositiveOrZero Integer posicionY, @NotNull @Positive Integer ancho,
            @NotNull @Positive Integer alto, @NotNull @PositiveOrZero Integer orden, @PositiveOrZero Long version) {
    }

    public record PoolVista(Long id, String nombre, TipoParticipante tipoParticipante, Boolean propietario,
            Boolean cajaNegra, Integer posicionX, Integer posicionY, Integer ancho, Integer alto,
            Integer orden, Long version) {
    }

    public record LaneDatos(@NotNull @Positive Long rolProcesoId, @NotNull @PositiveOrZero Integer orden,
            @NotNull @Positive Integer altura, @PositiveOrZero Long version) {
    }

    public record LaneVista(Long id, Long poolId, Long rolProcesoId, String nombre, Integer orden,
            Integer altura, Long version) {
    }

    public record OrdenLane(@NotNull @Positive Long id, @NotNull @PositiveOrZero Long version) {
    }

    public record Reordenar(@NotNull @Size(min = 1, max = 1000) List<@NotNull @Valid OrdenLane> lanes) {
    }

    public record OrdenPool(@NotNull @Positive Long id, @NotNull @PositiveOrZero Long version) {
    }

    public record ReordenarPools(@NotNull @Size(min = 1, max = 1000) List<@NotNull @Valid OrdenPool> pools) {
    }

    public enum ClaseNodo { ACTIVIDAD, GATEWAY, EVENTO }

    public record NodoDatos(@NotNull ClaseNodo clase, @NotNull @Positive Long poolId,
            @NotBlank @Size(max = 150) String nombre, @NotNull @PositiveOrZero Integer posicionX,
            @NotNull @PositiveOrZero Integer posicionY, @NotNull @Positive Integer ancho,
            @NotNull @Positive Integer alto, @Positive Long laneId, TipoActividad tipoActividad,
            TipoGateway tipoGateway, DireccionGateway direccionGateway, TipoEvento tipoEvento,
            NaturalezaEvento naturalezaEvento, OperacionEventoMensaje operacionMensaje) {
    }

    public record NodoVista(Long id, ClaseNodo clase, Long poolId, String nombre,
            Integer posicionX, Integer posicionY, Integer ancho, Integer alto, EstadoNodo estado,
            Long laneId, TipoActividad tipoActividad, TipoGateway tipoGateway, DireccionGateway direccionGateway,
            TipoEvento tipoEvento, NaturalezaEvento naturalezaEvento, OperacionEventoMensaje operacionMensaje) {
    }

    public record ArcoDatos(@NotNull @Positive Long origenId, @NotNull @Positive Long destinoId,
            @Size(max = 150) String etiqueta, @Size(max = 10000) String condicionSalida) {
    }

    public record ArcoVista(Long id, Long poolId, Long origenId, Long destinoId, String etiqueta, String condicionSalida) {
    }

    public record MensajeDetalle(MensajeDto.MensajeResponseDto mensaje,
            List<MensajeDto.CampoMensajeResponseDto> campos, List<MensajeDto.UsoMensajeActividadResponseDto> usos) {
    }

    public record Detalle(ProcesoDto.Ficha proceso, List<PoolVista> pools, List<LaneVista> lanes,
            List<NodoVista> nodos, List<ArcoVista> arcos, List<MensajeDetalle> mensajes,
            List<FlujoMensajeDto.FlujoMensajeResponseDto> flujos, List<String> advertencias) {
    }
}
