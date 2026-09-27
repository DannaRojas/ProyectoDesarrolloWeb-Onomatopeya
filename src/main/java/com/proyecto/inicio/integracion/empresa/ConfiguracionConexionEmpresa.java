package com.proyecto.inicio.integracion.empresa;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.crypto.password.Pbkdf2PasswordEncoder;

@Configuration
@Profile("conexion-empresa")
public class ConfiguracionConexionEmpresa {

    @Bean
    public PasswordEncoder codificadorConexionEmpresa() {
        return Pbkdf2PasswordEncoder.defaultsForSpringSecurity_v5_8();
    }
}
