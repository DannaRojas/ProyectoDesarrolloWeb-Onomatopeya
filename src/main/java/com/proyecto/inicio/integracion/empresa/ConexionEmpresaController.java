package com.proyecto.inicio.integracion.empresa;

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
public class ConexionEmpresaController {
    private final EmpresaService empresaService;
    private final ConexionEmpresaService conexion;

    public ConexionEmpresaController(EmpresaService empresaService, ConexionEmpresaService conexion) {
        this.empresaService = empresaService;
        this.conexion = conexion;
    }

    @PostMapping
    public ResponseEntity<EmpresaResponseDto> registrar(@Valid @RequestBody RegistroEmpresaConexion registro) {
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
                                         @Valid @RequestBody DatosEmpresaConexion datos) {
        return empresaService.actualizar(id, datos.alDtoExistente());
    }
}
