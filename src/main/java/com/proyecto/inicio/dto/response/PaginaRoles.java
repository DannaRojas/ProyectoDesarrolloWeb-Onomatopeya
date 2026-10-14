package com.proyecto.inicio.dto.response;

import java.util.List;

public record PaginaRoles(
        List<RolVista> contenido, int pagina, int tamanio, long total, int paginas) {}
