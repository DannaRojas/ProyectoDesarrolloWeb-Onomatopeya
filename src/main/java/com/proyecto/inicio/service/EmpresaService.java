package com.proyecto.inicio.service;

import com.proyecto.inicio.dto.request.EmpresaRequestDto;
import com.proyecto.inicio.dto.response.EmpresaResponseDto;
import com.proyecto.inicio.entity.Empresa;
import com.proyecto.inicio.repository.EmpresaRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class EmpresaService {

    private final EmpresaRepository empresaRepository;

    public EmpresaService(EmpresaRepository empresaRepository) {
        this.empresaRepository = empresaRepository;
    }

    @Transactional
    public EmpresaResponseDto crear(EmpresaRequestDto request) {
        
        validarDatosObligatorios(request);// se revisa que la info si venga completa

        // quitamos espacios que el usuario pudo escribir antes o después del nit
        String nit = request.getNit().trim();
    
        if (empresaRepository.existsByNit(nit)) {
            throw new IllegalArgumentException("Ya existe una empresa registrada con ese NIT");
        }

        // armamos la empresa con los datos que llegan desde el formulario
        Empresa empresa = new Empresa(
                null,
                request.getNombre().trim(),
                nit,
                request.getCorreoContacto().trim()
        );

        return mapearRespuesta(empresaRepository.save(empresa));
    }

    @Transactional(readOnly = true)
    public List<EmpresaResponseDto> consultarTodas() {
        // devolvemos dto para no exponer directamente la entidad de la base de datos
        return empresaRepository.findAll()
                .stream()
                .map(this::mapearRespuesta)
                .toList();
    }

    @Transactional(readOnly = true)
    public EmpresaResponseDto consultarPorId(Long id) {
        return mapearRespuesta(buscarEmpresa(id));
    }

    @Transactional
    public EmpresaResponseDto actualizar(Long id, EmpresaRequestDto request) {
        validarDatosObligatorios(request);

        Empresa empresa = buscarEmpresa(id);
        String nit = request.getNit().trim();

        // si cambió el nit, comprobamos que el nuevo tampoco esté ocupado
        if (!empresa.getNit().equals(nit) && empresaRepository.existsByNit(nit)) {
            throw new IllegalArgumentException("Ya existe una empresa registrada con ese NIT");
        }

        empresa.setNombre(request.getNombre().trim());
        empresa.setNit(nit);
        empresa.setCorreoContacto(request.getCorreoContacto().trim());

        return mapearRespuesta(empresaRepository.save(empresa));
    }

    @Transactional
    public void eliminar(Long id) {
        // primero buscamos la empresa para dar un error claro si no existe
        empresaRepository.delete(buscarEmpresa(id));
    }

    private Empresa buscarEmpresa(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("El id de la empresa es obligatorio");
        }

        return empresaRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Empresa no encontrada"));
    }

    private void validarDatosObligatorios(EmpresaRequestDto request) {
        if (request == null) {
            throw new IllegalArgumentException("Los datos de la empresa son obligatorios");
        }

        if (esVacio(request.getNombre()) || esVacio(request.getNit()) || esVacio(request.getCorreoContacto())) {
            throw new IllegalArgumentException("Nombre, NIT y correo de contacto son obligatorios");
        }

        if (!request.getCorreoContacto().trim().contains("@")) {
            throw new IllegalArgumentException("El correo de contacto no tiene un formato válido");
        }
    }

    private boolean esVacio(String valor) {
        return valor == null || valor.isBlank();
    }

    private EmpresaResponseDto mapearRespuesta(Empresa empresa) {
        // convertimos la entidad en el objeto que se entrega hacia afuera
        return new EmpresaResponseDto(
                empresa.getId(),
                empresa.getNombre(),
                empresa.getNit(),
                empresa.getCorreoContacto()
        );
    }
}
