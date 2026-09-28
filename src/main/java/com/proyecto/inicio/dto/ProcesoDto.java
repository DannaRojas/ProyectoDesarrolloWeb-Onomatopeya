package com.proyecto.inicio.dto;

import com.proyecto.inicio.entity.enums.*;
import jakarta.validation.constraints.*;
import java.time.OffsetDateTime;
import java.util.List;

public final class ProcesoDto {
    private ProcesoDto() {
    }

    public record Datos(@NotBlank @Size(max = 150) String nombre,
            @Size(max = 10000) String descripcion, @Size(max = 100) String categoria,
            @NotNull EstadoPublicacion estadoPublicacion, @PositiveOrZero Long version) {
    }

    public record Retirada(@NotNull @PositiveOrZero Long version, @NotNull @AssertTrue Boolean confirmar) {
    }

    public record Compartir(@NotNull @Positive Long empresaId, @NotNull Boolean permitido,
            @NotNull @PositiveOrZero Long version) {
    }

    public record Ficha(Long id, Long empresaId, Long creadorId, String nombre, String descripcion,
            String categoria, EstadoPublicacion estadoPublicacion, Boolean activo,
            OffsetDateTime fechaCreacion, Long version) {
    }

    public record Cambio(Long id, Long actorId, OffsetDateTime fecha, String tipoObjeto,
            Long objetoId, AccionHistorial accion, String descripcion) {
    }

    public record Pagina(List<Ficha> contenido, int pagina, int tamanio, long total, int paginas) {
    }

    public record Acceso(Long id, Long procesoId, Long empresaId, Boolean activo, Long version) {
    }
}
