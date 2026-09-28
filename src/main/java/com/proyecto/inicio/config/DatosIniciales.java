package com.proyecto.inicio.config;

import com.proyecto.inicio.dto.request.ProcesoRequestDto;
import com.proyecto.inicio.entity.*;
import com.proyecto.inicio.entity.enums.EstadoUsuario;
import com.proyecto.inicio.entity.enums.RolAcceso;
import com.proyecto.inicio.repository.*;
import com.proyecto.inicio.service.ProcesoService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.Pbkdf2PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.datos-demo", havingValue = "true")
public class DatosIniciales implements CommandLineRunner {
    private final EmpresaRepository empresas;
    private final UsuarioRepository usuarios;
    private final ProcesoRepository procesos;
    private final RolProcesoRepository roles;
    private final PoolRepository pools;
    private final LaneRepository lanes;
    private final ProcesoService procesoService;

    @Override
    @Transactional
    public void run(String... args) {
        prepararEmpresa("DEMO-001", "Empresa Demo Uno", "uno");
        prepararEmpresa("DEMO-002", "Empresa Demo Dos", "dos");
    }

    private void prepararEmpresa(String nit, String nombre, String sufijo) {
        Empresa empresa = empresas.findByNit(nit).orElseGet(() -> empresas.save(
                Empresa.builder().nit(nit).nombre(nombre)
                        .correoContacto("contacto." + sufijo + "@example.com").activo(true).build()));
        if (!nombre.equals(empresa.getNombre()) || !Boolean.TRUE.equals(empresa.getActivo())) {
            throw new IllegalStateException("El NIT de demostración ya pertenece a otra empresa o está inactivo.");
        }
        Usuario admin = usuario(empresa, "Administrador demo", "admin." + sufijo, RolAcceso.ADMINISTRADOR);
        usuario(empresa, "Editor demo", "editor." + sufijo, RolAcceso.EDITOR);
        usuario(empresa, "Lector demo", "lector." + sufijo, RolAcceso.LECTURA);

        Proceso proceso = procesos.findByEmpresaIdAndActivoTrue(empresa.getId()).stream()
                .filter(p -> p.getNombre().equals("Solicitud de compra demo")).findFirst()
                .orElseGet(() -> {
                    if (procesos.existsByEmpresaIdAndNombre(empresa.getId(), "Solicitud de compra demo")) {
                        throw new IllegalStateException("El proceso de demostración está retirado; no se reactiva.");
                    }
                    var respuesta = procesoService.crear(new ProcesoRequestDto(empresa.getId(), admin.getId(),
                            "Solicitud de compra demo", "Proceso ficticio para probar la entrega.", "Compras"));
                    return procesos.findById(respuesta.getId()).orElseThrow();
                });
        RolProceso rol = roles.findByEmpresaIdAndActivoTrue(empresa.getId()).stream()
                .filter(r -> r.getNombre().equals("Analista demo")).findFirst()
                .orElseGet(() -> {
                    if (roles.existsByEmpresaIdAndNombre(empresa.getId(), "Analista demo")) {
                        throw new IllegalStateException("El rol de demostración está retirado; no se reactiva.");
                    }
                    return roles.save(RolProceso.builder().empresa(empresa).nombre("Analista demo")
                            .descripcion("Revisa solicitudes de ejemplo.").activo(true).build());
                });
        Pool pool = pools.findByProcesoIdAndActivoTrue(proceso.getId()).stream()
                .filter(p -> Boolean.TRUE.equals(p.getPropietario())).findFirst()
                .orElseThrow(() -> new IllegalStateException("El proceso demo no tiene pool propietario activo."));
        if (lanes.findByPoolIdAndActivoTrueOrderByOrden(pool.getId()).isEmpty()) {
            if (lanes.findAll().stream().anyMatch(l -> l.getPool().getId().equals(pool.getId()))) {
                throw new IllegalStateException("El pool demo ya tiene lanes retiradas; no se reemplazan.");
            }
            lanes.save(Lane.builder().pool(pool).rolProceso(rol).orden(1).altura(300).activo(true).build());
        }
    }

    private Usuario usuario(Empresa empresa, String nombre, String prefijo, RolAcceso rol) {
        String correo = prefijo + "@example.com";
        Usuario existente = usuarios.findByCorreo(correo).orElse(null);
        if (existente != null) {
            if (!existente.getEmpresa().getId().equals(empresa.getId())) {
                throw new IllegalStateException("El correo demo pertenece a otra empresa.");
            }
            return existente;
        }
        // Esta contraseña es únicamente para las cuentas ficticias de la demostración.
        String hash = Pbkdf2PasswordEncoder.defaultsForSpringSecurity_v5_8().encode("DemoEntrega1!2026");
        return usuarios.save(Usuario.builder().empresa(empresa).nombre(nombre).correo(correo)
                .passwordHash(hash).rolAcceso(rol).estado(EstadoUsuario.ACTIVO).build());
    }
}
