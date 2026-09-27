package com.proyecto.inicio.entity;

import com.proyecto.inicio.entity.enums.*;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "mensaje")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@NamedQuery(name = "Mensaje.listarActivosPorProceso", query = "select m from Mensaje m where m.nodo.proceso.id = :procesoId and m.activo = true order by m.id")
public class Mensaje {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "nodo_id", nullable = false, unique = true)
    private Nodo nodo;

    @Enumerated(EnumType.STRING) @Column(nullable = false)
    private SentidoMensaje sentido;

    @Column(nullable = false, length = 150)
    private String nombre;

    @Builder.Default @Column(name = "origen_externo", nullable = false)
    private Boolean origenExterno = false;

    @Enumerated(EnumType.STRING)
    private TipoCorrelacion correlacionTipo;

    @Column(length = 150)
    private String correlacionNegocio;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "correlacion_campo_id")
    private CampoMensaje correlacionCampo;

    @Enumerated(EnumType.STRING)
    private PoliticaSinCorrespondencia politicaSinCorrespondencia;

    @Builder.Default
    @Column(nullable = false)
    private Boolean activo = true;

    @Version
    private Long version;
}
