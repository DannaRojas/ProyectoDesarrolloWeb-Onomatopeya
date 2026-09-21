package com.proyecto.inicio.controller;

import com.proyecto.inicio.dto.request.EmpresaRequestDto;
import com.proyecto.inicio.dto.response.EmpresaResponseDto;
import com.proyecto.inicio.service.EmpresaService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/empresas")
public class EmpresaController {

    private final EmpresaService empresaService;

    public EmpresaController(EmpresaService empresaService) {
        this.empresaService = empresaService;
    }

    // CREAR UNA EMPRESA
    @PostMapping
    public ResponseEntity<EmpresaResponseDto> crear(
            @RequestBody EmpresaRequestDto request) {

        return ResponseEntity
                .status(201)
                .body(empresaService.crear(request));
    }

    // CONSULTAR TODAS LAS EMPRESAS
    @GetMapping
    public ResponseEntity<List<EmpresaResponseDto>> consultarTodas() {

        return ResponseEntity.ok(
                empresaService.consultarTodas()
        );
    }

    // CONSULTAR UNA EMPRESA POR ID
    @GetMapping("/{id}")
    public ResponseEntity<EmpresaResponseDto> consultarPorId(
            @PathVariable Long id) {

        return ResponseEntity.ok(
                empresaService.consultarPorId(id)
        );
    }

    // ACTUALIZAR UNA EMPRESA
    @PutMapping("/{id}")
    public ResponseEntity<EmpresaResponseDto> actualizar(
            @PathVariable Long id,
            @RequestBody EmpresaRequestDto request) {

        return ResponseEntity.ok(
                empresaService.actualizar(id, request)
        );
    }

    // ELIMINAR UNA EMPRESA
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(
            @PathVariable Long id) {

        empresaService.eliminar(id);

        return ResponseEntity.noContent().build();
    }
}

