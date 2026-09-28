package com.proyecto.inicio.controller;

import com.proyecto.inicio.dto.RegistroEmpresaDto;
import com.proyecto.inicio.dto.RegistroEmpresaDto.DatosEmpresa;
import com.proyecto.inicio.service.RegistroEmpresaService;

import com.proyecto.inicio.dto.response.EmpresaResponseDto;
import com.proyecto.inicio.service.EmpresaService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.util.List;

@RestController
@Profile("conexion-empresa")
@RequestMapping("/empresas")
public class EmpresaController {
    private final EmpresaService empresaService;
    private final RegistroEmpresaService conexion;

    public EmpresaController(EmpresaService empresaService, RegistroEmpresaService conexion) {
        this.empresaService = empresaService;
        this.conexion = conexion;
    }

    @PostMapping
    public ResponseEntity<EmpresaResponseDto> registrar(@Valid @RequestBody RegistroEmpresaDto registro) {
        var empresa = conexion.registrar(registro);
        var ubicacion = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(empresa.getId()).toUri();
        return ResponseEntity.created(ubicacion).body(empresa);
    }

    @GetMapping
    public List<EmpresaResponseDto> listar() {
        return empresaService.consultarTodas();
    }

    @GetMapping("/{id}")
    public EmpresaResponseDto consultar(@Positive @PathVariable Long id) {
        return empresaService.consultarPorId(id);
    }

    @PutMapping("/{id}")
    public EmpresaResponseDto actualizar(@Positive @PathVariable Long id,
                                         @Valid @RequestBody DatosEmpresa datos) {
        return empresaService.actualizar(id, datos.alDtoExistente());
    }
}
