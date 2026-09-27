package com.proyecto.inicio.entity;

import com.proyecto.inicio.entity.enums.*;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "permiso_estructura", uniqueConstraints = @UniqueConstraint(name = "uk_permiso_estructura", columnNames = {"proceso_id", "rol_acceso", "recurso", "accion"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PermisoEstructura {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "proceso_id", nullable = false)
    private Proceso proceso;

    @Enumerated(EnumType.STRING) @Column(name = "rol_acceso", nullable = false)
    private RolAcceso rolAcceso;

    @Enumerated(EnumType.STRING) @Column(nullable = false)
    private RecursoEstructura recurso;

    @Enumerated(EnumType.STRING) @Column(nullable = false)
    private AccionEstructura accion;

    @Column(nullable = false)
    private Boolean permitido;

    @Version
    private Long version;
}
