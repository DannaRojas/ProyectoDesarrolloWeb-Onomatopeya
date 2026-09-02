package com.proyecto.inicio.controller;

import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

@RestController
@RequestMapping("/empresas")
public class EmpresaController {

    // CONSULTAR TODAS LAS EMPRESAS
    @GetMapping
    public String getEmpresas() {
        return "Lista de empresas";
    }

    // CONSULTAR UNA EMPRESA
    @GetMapping("/{id}")
    public String getEmpresa(@PathVariable Long id) {
        return "Empresa con id: " + id;
    }

    // CREAR UNA EMPRESA
    @PostMapping
    public String crearEmpresa(@RequestBody String empresa) {
        return "Empresa creada: " + empresa;
    }

    // ACTUALIZAR UNA EMPRESA
    @PutMapping("/{id}")
    public String actualizarEmpresa(
            @PathVariable Long id,
            @RequestBody String empresa) {

        return "Empresa con id " + id + " actualizada: " + empresa;
    }

    // ELIMINAR UNA EMPRESA
    @DeleteMapping("/{id}")
    public String eliminarEmpresa(@PathVariable Long id) {
        return "Empresa con id " + id + " eliminada";
    }
}

