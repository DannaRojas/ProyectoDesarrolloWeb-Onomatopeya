package com.proyecto.inicio.config;

import com.proyecto.inicio.dto.request.*;
import com.proyecto.inicio.dto.MensajeDto.*;
import com.proyecto.inicio.dto.FlujoMensajeDto.*;
import com.proyecto.inicio.dto.PermisoEstructuraDto.*;
import com.proyecto.inicio.dto.response.*;
import com.proyecto.inicio.entity.*;
import org.modelmapper.ModelMapper;
import org.modelmapper.convention.MatchingStrategies;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.crypto.password.Pbkdf2PasswordEncoder;

@Configuration
public class Configuracion {
    @Bean
    public ModelMapper modelMapper() {
        ModelMapper mapper = new ModelMapper();
        mapper.getConfiguration()
                .setMatchingStrategy(MatchingStrategies.STRICT)
                .setPreferNestedProperties(false);
        configurarEntradas(mapper);
        configurarRespuestas(mapper);
        mapper.validate();
        return mapper;
    }

    private void configurarEntradas(ModelMapper mapper) {
        // La empresa, el estado y los datos de invitación se deciden en el servicio.
        mapper.createTypeMap(InvitarUsuarioRequestDto.class, Usuario.class).addMappings(m -> {
            m.skip(Usuario::setId);
            m.skip(Usuario::setEmpresa);
            m.skip(Usuario::setPasswordHash);
            m.skip(Usuario::setEstado);
            m.skip(Usuario::setFechaCreacion);
            m.skip(Usuario::setInvitadoPor);
            m.skip(Usuario::setTokenInvitacionHash);
            m.skip(Usuario::setInvitacionExpiraEn);
            m.skip(Usuario::setVersion);
        });
        // Los IDs de relaciones se validan y buscan en Service, no se convierten en entidades nuevas.
        mapper.createTypeMap(MensajeRequestDto.class, Mensaje.class).addMappings(m -> {
            m.skip(Mensaje::setId);
            m.skip(Mensaje::setVersion);
            m.skip(Mensaje::setActivo);
            m.skip(Mensaje::setNodo);
            m.skip(Mensaje::setCorrelacionCampo);
        });
        mapper.createTypeMap(CampoMensajeRequestDto.class, CampoMensaje.class).addMappings(m -> {
            m.skip(CampoMensaje::setId);
            m.skip(CampoMensaje::setVersion);
            m.skip(CampoMensaje::setActivo);
            m.skip(CampoMensaje::setMensaje);
        });
        mapper.createTypeMap(FlujoMensajeRequestDto.class, FlujoMensaje.class).addMappings(m -> {
            m.skip(FlujoMensaje::setId);
            m.skip(FlujoMensaje::setVersion);
            m.skip(FlujoMensaje::setActivo);
            m.skip(FlujoMensaje::setProceso);
            m.skip(FlujoMensaje::setPoolOrigen);
            m.skip(FlujoMensaje::setPoolDestino);
            m.skip(FlujoMensaje::setNodoOrigen);
            m.skip(FlujoMensaje::setNodoDestino);
            m.skip(FlujoMensaje::setActividadError);
        });
        mapper.createTypeMap(PermisoEstructuraRequestDto.class, PermisoEstructura.class).addMappings(m -> {
            m.skip(PermisoEstructura::setId);
            m.skip(PermisoEstructura::setVersion);
            m.skip(PermisoEstructura::setProceso);
        });
        // UsoMensajeActividadRequestDto solo trae una FK: el servicio resuelve y valida esa relación.
    }

    private void configurarRespuestas(ModelMapper mapper) {
        // Las respuestas muestran el ID de la relación, no el objeto JPA completo.
        mapper.createTypeMap(Usuario.class, UsuarioResponseDto.class).addMappings(m ->
                m.map(origen -> origen.getEmpresa().getId(), UsuarioResponseDto::setEmpresaId));
        mapper.createTypeMap(Mensaje.class, MensajeResponseDto.class).addMappings(m -> {
            m.map(origen -> origen.getNodo().getId(), MensajeResponseDto::setNodoId);
            m.map(origen -> origen.getCorrelacionCampo().getId(), MensajeResponseDto::setCorrelacionCampoId);
        });
        mapper.createTypeMap(CampoMensaje.class, CampoMensajeResponseDto.class).addMappings(m ->
                m.map(origen -> origen.getMensaje().getId(), CampoMensajeResponseDto::setMensajeId));
        mapper.createTypeMap(FlujoMensaje.class, FlujoMensajeResponseDto.class).addMappings(m -> {
            m.map(origen -> origen.getProceso().getId(), FlujoMensajeResponseDto::setProcesoId);
            m.map(origen -> origen.getPoolOrigen().getId(), FlujoMensajeResponseDto::setPoolOrigenId);
            m.map(origen -> origen.getPoolDestino().getId(), FlujoMensajeResponseDto::setPoolDestinoId);
            m.map(origen -> origen.getNodoOrigen().getId(), FlujoMensajeResponseDto::setNodoOrigenId);
            m.map(origen -> origen.getNodoDestino().getId(), FlujoMensajeResponseDto::setNodoDestinoId);
            m.map(origen -> origen.getActividadError().getId(), FlujoMensajeResponseDto::setActividadErrorId);
        });
        mapper.createTypeMap(UsoMensajeActividad.class, UsoMensajeActividadResponseDto.class).addMappings(m -> {
            m.map(origen -> origen.getMensaje().getId(), UsoMensajeActividadResponseDto::setMensajeId);
            m.map(origen -> origen.getActividad().getId(), UsoMensajeActividadResponseDto::setActividadId);
        });
        mapper.createTypeMap(PermisoEstructura.class, PermisoEstructuraResponseDto.class).addMappings(m ->
                m.map(origen -> origen.getProceso().getId(), PermisoEstructuraResponseDto::setProcesoId));
    }

    @Bean
    @Profile("conexion-empresa")
    public PasswordEncoder codificadorConexionEmpresa() {
        return Pbkdf2PasswordEncoder.defaultsForSpringSecurity_v5_8();
    }
}
