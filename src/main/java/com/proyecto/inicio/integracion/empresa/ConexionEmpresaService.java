package com.proyecto.inicio.integracion.empresa;

import com.proyecto.inicio.dto.response.EmpresaResponseDto;
import com.proyecto.inicio.service.EmpresaService;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@Profile("conexion-empresa")
public class ConexionEmpresaService {
    private final EmpresaService empresaService;
    private final PasswordEncoder codificador;

    public ConexionEmpresaService(EmpresaService empresaService, PasswordEncoder codificador) {
        this.empresaService = empresaService;
        this.codificador = codificador;
    }

    public EmpresaResponseDto registrar(RegistroEmpresaConexion registro) {
        var admin = registro.administrador();
        // El servicio existente recibe el hash y guarda ambos registros en su transacción.
        return empresaService.crearEmpresaConAdminInicial(
                registro.empresa().alDtoExistente(), admin.nombre().strip(),
                admin.correo().strip(), codificador.encode(admin.contrasena()));
    }
}
