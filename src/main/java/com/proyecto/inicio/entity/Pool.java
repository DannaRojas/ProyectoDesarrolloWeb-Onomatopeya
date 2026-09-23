package com.proyecto.inicio.entity;

import com.proyecto.inicio.entity.enums.TipoParticipante;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "pool")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Pool {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "proceso_id", nullable = false)
    private Proceso proceso;

    @Column(nullable = false, length = 150)
    private String nombre;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_participante", nullable = false)
    private TipoParticipante tipoParticipante;

    @Column(nullable = false)
    private Boolean propietario = false;

    @Column(name = "caja_negra", nullable = false)
    private Boolean cajaNegra = false;

    @Column(name = "posicion_x", nullable = false)
    private Integer posicionX;

    @Column(name = "posicion_y", nullable = false)
    private Integer posicionY;

    @Column(nullable = false)
    private Integer ancho;

    @Column(nullable = false)
    private Integer alto;

    @Column(nullable = false)
    private Integer orden;

    @Column(nullable = false)
    private Boolean activo = true;

    @Version
    private Long version;
}