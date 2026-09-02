package com.proyecto.inicio.dto.response;

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
}
