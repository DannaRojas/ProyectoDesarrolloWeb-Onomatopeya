package com.proyecto.inicio.entity;

import com.proyecto.inicio.entity.enums.*;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "campo_mensaje", uniqueConstraints = @UniqueConstraint(name = "uk_campo_mensaje_nombre", columnNames = {"mensaje_id", "nombre"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CampoMensaje {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "mensaje_id", nullable = false)
    private Mensaje mensaje;

    @Column(nullable = false, length = 100)
    private String nombre;

    @Enumerated(EnumType.STRING) @Column(nullable = false)
    private TipoDatoMensaje tipoDato;

    @Column(nullable = false)
    private Integer orden;

    @Builder.Default
    @Column(nullable = false)
    private Boolean activo = true;

    @Version
    private Long version;
}
