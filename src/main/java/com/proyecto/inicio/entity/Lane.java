package com.proyecto.inicio.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
        name = "lane",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_lane_pool_orden",
                columnNames = {"pool_id", "orden"}
        )
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Lane {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "pool_id", nullable = false)
    private Pool pool;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "rol_proceso_id", nullable = false)
    private RolProceso rolProceso;

    @Column(nullable = false)
    private Integer orden;

    @Column(nullable = false)
    private Integer altura;

    @Column(nullable = false)
    private Boolean activo = true;

    @Version
    private Long version;
}