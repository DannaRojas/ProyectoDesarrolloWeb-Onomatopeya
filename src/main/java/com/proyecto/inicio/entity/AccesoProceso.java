package com.proyecto.inicio.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.OffsetDateTime;


@Entity
@Table(name = "acceso_proceso")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AccesoProceso {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Añadir cuando proceso ya este completa 
    //@ManyToOne(optional = false, fetch = FetchType.LAZY)
    //@JoinColumn(name = "proceso_id", nullable = false)
    //private Proceso proceso;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "empresa_invitada_id", nullable = false)
    private Empresa empresaInvitada;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "otorgado_por_id", nullable = false)
    private Usuario otorgadoPor;

    @Column(name = "otorgado_en", nullable = false)
    private OffsetDateTime otorgadoEn;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "revocado_por_id")
    private Usuario revocadoPor;

    @Column(name = "revocado_en")
    private OffsetDateTime revocadoEn;

    @Column(nullable = false)
    private Boolean activo = true;

    @Version
    private Long version;

    @PrePersist
    protected void onCreate() {
        if (otorgadoEn == null) {
            otorgadoEn = OffsetDateTime.now();
        }
    }
}
