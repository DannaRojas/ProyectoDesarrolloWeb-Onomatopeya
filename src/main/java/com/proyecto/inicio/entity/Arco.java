package com.proyecto.inicio.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "arco", uniqueConstraints = @UniqueConstraint(name = "uk_arco_origen_destino", columnNames = {"origen_id", "destino_id"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Arco {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "proceso_id", nullable = false)
    private Proceso proceso;
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "pool_id", nullable = false)
    private Pool pool;
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "origen_id", nullable = false)
    private Nodo origen;
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "destino_id", nullable = false)
    private Nodo destino;

    @Column(length = 150)
    private String etiqueta;
    @Column(name = "condicion_salida", columnDefinition = "TEXT")
    private String condicionSalida;
    @Column(nullable = false)
    private Boolean activo = true;
}
