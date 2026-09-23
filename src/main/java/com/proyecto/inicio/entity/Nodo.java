package com.proyecto.inicio.entity;

import com.proyecto.inicio.entity.enums.EstadoNodo;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "nodo")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "tipo_nodo", discriminatorType = DiscriminatorType.STRING, length = 20)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public abstract class Nodo {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "proceso_id", nullable = false)
    private Proceso proceso;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "pool_id", nullable = false)
    private Pool pool;

    @Column(nullable = false, length = 150)
    private String nombre;

    @Column(name = "posicion_x", nullable = false)
    private Integer posicionX;
    @Column(name = "posicion_y", nullable = false)
    private Integer posicionY;
    @Column(nullable = false)
    private Integer ancho;
    @Column(nullable = false)
    private Integer alto;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoNodo estado = EstadoNodo.BORRADOR;
}
