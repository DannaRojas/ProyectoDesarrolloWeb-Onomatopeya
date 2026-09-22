package com.proyecto.inicio.entity;

import com.proyecto.inicio.entity.enums.AccionHistorial;
import com.proyecto.inicio.entity.enums.TipoActor;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "historial_cambio")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HistorialCambio {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "operacion_id", nullable = false)
    private UUID operacionId;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "empresa_contexto_id", nullable = false)
    private Empresa empresaContexto;

    // Añadir cuando proceso ya este completa
    //@ManyToOne(fetch = FetchType.LAZY)
    //@JoinColumn(name = "proceso_id")
    //private Proceso proceso;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "actor_id")
    private Usuario actor;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_actor", nullable = false)
    private TipoActor tipoActor;

    @Column(nullable = false)
    private OffsetDateTime fecha;

    @Column(name = "tipo_objeto", nullable = false)
    private String tipoObjeto;

    @Column(name = "objeto_id", nullable = false)
    private Long objetoId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AccionHistorial accion;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private String antes;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private String despues;

    private String descripcion;

    @PrePersist
    protected void onCreate() {
        if (fecha == null) {
            fecha = OffsetDateTime.now();
        }
        if (operacionId == null) {
            operacionId = UUID.randomUUID();
        }
    }
}
