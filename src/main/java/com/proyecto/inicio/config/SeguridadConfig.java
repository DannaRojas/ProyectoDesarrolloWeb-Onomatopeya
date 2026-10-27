package com.proyecto.inicio.config;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import com.proyecto.inicio.entity.enums.EstadoUsuario;
import com.proyecto.inicio.repository.UsuarioRepository;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Locale;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@Profile("conexion-empresa")
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class SeguridadConfig {
    @Bean
    SecurityFilterChain filtros(HttpSecurity http) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(sesion ->
                        sesion.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(rutas -> rutas
                        .requestMatchers(HttpMethod.POST,
                                "/auth/login",
                                "/empresas",
                                "/usuarios/invitaciones/aceptar")
                        .permitAll()
                        .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html")
                        .permitAll()
                        .anyRequest()
                        .authenticated())
                .oauth2ResourceServer(oauth -> oauth.jwt(Customizer.withDefaults()))
                .build();
    }

    @Bean
    UserDetailsService usuariosAutenticables(UsuarioRepository usuarios) {
        return correo -> usuarios
                .buscarPorCorreoConEmpresa(correo.strip().toLowerCase(Locale.ROOT))
                .filter(usuario -> usuario.getEstado() == EstadoUsuario.ACTIVO)
                .filter(usuario -> Boolean.TRUE.equals(usuario.getEmpresa().getActivo()))
                .filter(usuario -> usuario.getPasswordHash() != null
                        && !usuario.getPasswordHash().isBlank())
                .map(usuario -> User.withUsername(usuario.getCorreo())
                        .password(usuario.getPasswordHash())
                        .roles(usuario.getRolAcceso().name())
                        .build())
                .orElseThrow(() -> new UsernameNotFoundException("Credenciales inválidas."));
    }

    @Bean
    AuthenticationManager autenticador(
            UserDetailsService usuariosAutenticables, PasswordEncoder codificador) {
        DaoAuthenticationProvider proveedor = new DaoAuthenticationProvider(usuariosAutenticables);
        proveedor.setPasswordEncoder(codificador);
        return new ProviderManager(proveedor);
    }

    @Bean
    SecretKey claveJwt(@Value("${app.jwt.secret:}") String configurada) {
        byte[] bytes;
        if (configurada.isBlank()) {
            bytes = new byte[32];
            new SecureRandom().nextBytes(bytes);
        } else {
            bytes = configurada.getBytes(StandardCharsets.UTF_8);
            if (bytes.length < 32) {
                throw new IllegalArgumentException("APP_JWT_SECRET debe tener al menos 32 bytes.");
            }
        }
        return new SecretKeySpec(bytes, "HmacSHA256");
    }

    @Bean
    JwtEncoder firmador(SecretKey claveJwt) {
        return new NimbusJwtEncoder(new ImmutableSecret<>(claveJwt));
    }

    @Bean
    JwtDecoder verificador(SecretKey claveJwt) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder
                .withSecretKey(claveJwt)
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
        decoder.setJwtValidator(JwtValidators.createDefaultWithIssuer("onomatopeya"));
        return decoder;
    }
}
