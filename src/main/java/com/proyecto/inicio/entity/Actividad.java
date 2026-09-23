package com.proyecto.inicio.entity;

import com.proyecto.inicio.entity.enums.TipoActividad;
import jakarta.persistence.*;
import lombok.*;

@Entity
@DiscriminatorValue("ACTIVIDAD")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class Actividad extends Nodo {
    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_actividad", length = 20, nullable = false)
    private TipoActividad tipo;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "lane_id", nullable = false)
    private Lane lane;
}
