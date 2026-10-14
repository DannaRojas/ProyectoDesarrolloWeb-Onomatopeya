package com.proyecto.inicio.entity;

import com.proyecto.inicio.entity.enums.PoliticaFallo;
import com.proyecto.inicio.entity.enums.TipoDestinoExterno;

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
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "flujo_mensaje")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FlujoMensaje {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "proceso_id", nullable = false)
    private Proceso proceso;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "pool_origen_id", nullable = false)
    private Pool poolOrigen;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "pool_destino_id", nullable = false)
    private Pool poolDestino;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "nodo_origen_id")
    private Nodo nodoOrigen;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "nodo_destino_id")
    private Nodo nodoDestino;

    @Enumerated(EnumType.STRING)
    private TipoDestinoExterno tipoDestino;

    @Enumerated(EnumType.STRING)
    private PoliticaFallo politicaFallo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "actividad_error_id")
    private Actividad actividadError;

    @Builder.Default
    @Column(nullable = false)
    private Boolean activo = true;

    @Version
    private Long version;
}
