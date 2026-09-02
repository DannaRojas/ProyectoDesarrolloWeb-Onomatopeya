package com.proyecto.inicio.controller;

import com.proyecto.inicio.dto.request.EmpresaRequestDto;
import com.proyecto.inicio.dto.response.EmpresaResponseDto;
import com.proyecto.inicio.service.EmpresaService;

import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/empresas")
public class EmpresaController {

    private final EmpresaService empresaService;

    public EmpresaController(EmpresaService empresaService) {
        this.empresaService = empresaService;
    }

    // CONSULTAR TODAS LAS EMPRESAS
    @GetMapping
    public List<EmpresaResponseDto> getEmpresas() {
        return empresaService.consultarTodas();
    }

    // CONSULTAR UNA EMPRESA
    @GetMapping("/{id}")
    public EmpresaResponseDto getEmpresa(@PathVariable Long id) {
        return empresaService.consultarPorId(id);
    }

    // CREAR UNA EMPRESA
    @PostMapping
    public EmpresaResponseDto crearEmpresa(
            @RequestBody EmpresaRequestDto request) {

        return empresaService.crear(request);
    }

    // ACTUALIZAR UNA EMPRESA
    @PutMapping("/{id}")
    public EmpresaResponseDto actualizarEmpresa(
            @PathVariable Long id,
            @RequestBody EmpresaRequestDto request) {

        return empresaService.actualizar(id, request);
    }

    // ELIMINAR UNA EMPRESA
    @DeleteMapping("/{id}")
    public void eliminarEmpresa(@PathVariable Long id) {
        empresaService.eliminar(id);
    }
}