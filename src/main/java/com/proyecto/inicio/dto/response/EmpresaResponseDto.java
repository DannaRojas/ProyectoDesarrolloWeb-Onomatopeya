package com.proyecto.inicio.dto.response;

import java.time.OffsetDateTime;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class EmpresaResponseDto {

    
    private Long id;
    private String nombre;
    private String nit;
    private String correoContacto;
    private Boolean activo;
    private OffsetDateTime fechaCreacion;
}