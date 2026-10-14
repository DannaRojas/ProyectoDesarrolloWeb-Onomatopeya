package com.proyecto.inicio.entity;

import com.proyecto.inicio.entity.enums.PoliticaSinCorrespondencia;
import com.proyecto.inicio.entity.enums.SentidoMensaje;
import com.proyecto.inicio.entity.enums.TipoCorrelacion;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "mensaje")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@NamedQuery(
        name = "Mensaje.listarActivosPorProceso",
        query =
                "select m from Mensaje m where m.nodo.proceso.id = :procesoId and m.activo = true"
                    + " order by m.id")
public class Mensaje {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "nodo_id", nullable = false, unique = true)
    private Nodo nodo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SentidoMensaje sentido;

    @Column(nullable = false, length = 150)
    private String nombre;

    @Builder.Default
    @Column(name = "origen_externo", nullable = false)
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
