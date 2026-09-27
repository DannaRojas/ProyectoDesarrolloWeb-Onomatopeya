package com.proyecto.inicio.entity;

import com.proyecto.inicio.entity.enums.*;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "uso_mensaje_actividad", uniqueConstraints = @UniqueConstraint(name = "uk_uso_mensaje_actividad", columnNames = {"mensaje_id", "actividad_id"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class UsoMensajeActividad {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "mensaje_id", nullable = false)
    private Mensaje mensaje;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "actividad_id", nullable = false)
    private Actividad actividad;

    @Builder.Default
    @Column(nullable = false)
    private Boolean activo = true;

    @Version
    private Long version;
}
