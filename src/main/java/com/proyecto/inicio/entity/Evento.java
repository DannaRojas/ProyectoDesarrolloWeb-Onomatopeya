package com.proyecto.inicio.entity;

import com.proyecto.inicio.entity.enums.NaturalezaEvento;
import com.proyecto.inicio.entity.enums.OperacionEventoMensaje;
import com.proyecto.inicio.entity.enums.TipoEvento;
import jakarta.persistence.*;
import lombok.*;

@Entity
@DiscriminatorValue("EVENTO")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class Evento extends Nodo {
    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_evento", length = 20)
    private TipoEvento tipo;

    @Enumerated(EnumType.STRING)
    @Column(name = "naturaleza_evento", length = 20)
    private NaturalezaEvento naturaleza;

    @Enumerated(EnumType.STRING)
    @Column(name = "operacion_mensaje", length = 20)
    private OperacionEventoMensaje operacionMensaje;
}
