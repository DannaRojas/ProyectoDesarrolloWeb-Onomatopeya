package com.proyecto.inicio.dto.response;

import java.util.List;

public record Pagina(List<Ficha> contenido, int pagina, int tamanio, long total, int paginas) {}
