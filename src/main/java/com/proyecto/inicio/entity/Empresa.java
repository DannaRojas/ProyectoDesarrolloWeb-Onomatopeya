package com.proyecto.inicio.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;



@Entity
@Table(name = "empresas")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Empresa {

     @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String nombre;

    @Column(nullable = false, length = 30)
    private String nit;

    @Column(name = "correo_contacto", nullable = false, length = 254)
    private String correoContacto;

    @Column(nullable = false)
    private Boolean activo = true;

    @Column( name = "fecha_creacion",nullable = false, insertable = false, updatable = false)
    private OffsetDateTime fechaCreacion;
    
}
