package com.proyecto.inicio.dto.response;

import com.proyecto.inicio.entity.enums.EstadoUsuario;
import com.proyecto.inicio.entity.enums.RolAcceso;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.OffsetDateTime;

@Getter
@Setter
@NoArgsConstructor
public class UsuarioResponseDto {
    private Long id;
    private Long empresaId;
    private String nombre;
    private String correo;
    private RolAcceso rolAcceso;
    private EstadoUsuario estado;
    private OffsetDateTime fechaCreacion;
    private Long version;
}
