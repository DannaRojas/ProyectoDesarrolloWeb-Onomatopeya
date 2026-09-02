package com.proyecto.inicio.controller;

import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@RestController
@RequestMapping("/empresas")
public class EmpresaController {

    @GetMapping
    public String getEmpresas() {
        return "Lista de empresas";
    }

    @GetMapping("/{id}")
    public String getEmpresa(@PathVariable Long id) {
        return "Empresa con id: " + id;
    }
}

