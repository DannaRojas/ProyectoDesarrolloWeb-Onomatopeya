package com.proyecto.inicio.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
        name = "rol_proceso",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_rol_proceso_empresa_nombre",
                columnNames = {"empresa_id", "nombre"}
        )
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RolProceso {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "empresa_id", nullable = false)
    private Empresa empresa;

    @Column(nullable = false, length = 150)
    private String nombre;

    @Column(columnDefinition = "TEXT")
    private String descripcion;

    @Column(nullable = false)
    private Boolean activo = true;

    @Version
    private Long version;
}