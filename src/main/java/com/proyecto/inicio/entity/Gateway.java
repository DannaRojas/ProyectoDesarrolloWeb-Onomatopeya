package com.proyecto.inicio.entity;

import com.proyecto.inicio.entity.enums.DireccionGateway;
import com.proyecto.inicio.entity.enums.TipoGateway;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@DiscriminatorValue("GATEWAY")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Gateway extends Nodo {
    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_gateway", length = 20)
    private TipoGateway tipo;

    @Enumerated(EnumType.STRING)
    @Column(name = "direccion_gateway", length = 20)
    private DireccionGateway direccion;
}
