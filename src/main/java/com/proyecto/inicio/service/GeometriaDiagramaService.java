package com.proyecto.inicio.service;

import com.proyecto.inicio.entity.Actividad;
import com.proyecto.inicio.entity.Lane;
import com.proyecto.inicio.entity.Nodo;
import com.proyecto.inicio.entity.Pool;
import com.proyecto.inicio.repository.LaneRepository;
import com.proyecto.inicio.repository.NodoRepository;
import lombok.RequiredArgsConstructor;
import org.hibernate.Hibernate;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class GeometriaDiagramaService {
    private final LaneRepository lanes;
    private final NodoRepository nodos;

    public Map<Long, Integer> posiciones(Pool pool) {
        Map<Long, Integer> posiciones = new LinkedHashMap<>();
        int inicio = 0;
        for (Lane lane : lanes.findByPoolIdAndActivoTrueOrderByOrden(pool.getId())) {
            posiciones.put(lane.getId(), inicio);
            if (lane.getAltura() == null || lane.getAltura() <= 0
                    || (long) inicio + lane.getAltura() > Integer.MAX_VALUE) {
                throw new IllegalArgumentException("La altura acumulada de las lanes no es válida.");
            }
            inicio += lane.getAltura();
        }
        return posiciones;
    }

    public int trasladar(int y, int inicioAnterior, Lane destino, int alto) {
        if (alto > destino.getAltura()) {
            throw new IllegalArgumentException("La actividad no cabe en la altura de la lane.");
        }
        int inicio = posiciones(destino.getPool()).get(destino.getId());
        int desplazamiento = Math.max(0, Math.min(y - inicioAnterior, destino.getAltura() - alto));
        return inicio + desplazamiento;
    }

    public void validarNodo(Nodo nodo) {
        Pool pool = nodo.getPool();
        if (nodo.getPosicionX() == null || nodo.getPosicionY() == null
                || nodo.getAncho() == null || nodo.getAlto() == null
                || pool.getAncho() == null || pool.getAlto() == null
                || nodo.getPosicionX() < 0 || nodo.getPosicionY() < 0
                || nodo.getAncho() <= 0 || nodo.getAlto() <= 0
                || (long) nodo.getPosicionX() + nodo.getAncho() > pool.getAncho()
                || (long) nodo.getPosicionY() + nodo.getAlto() > pool.getAlto()) {
            throw new IllegalArgumentException("El nodo debe quedar completamente dentro del pool.");
        }
        if (nodo instanceof Actividad actividad) {
            Lane lane = actividad.getLane();
            Integer inicio = posiciones(pool).get(lane.getId());
            if (inicio == null || nodo.getPosicionY() < inicio
                    || (long) nodo.getPosicionY() + nodo.getAlto() > (long) inicio + lane.getAltura()) {
                throw new IllegalArgumentException("La actividad debe quedar dentro de su lane.");
            }
        }
    }

    public void validarPool(Pool pool) {
        long altura = lanes.findByPoolIdAndActivoTrueOrderByOrden(pool.getId()).stream()
                .mapToLong(Lane::getAltura).sum();
        if (pool.getAlto() == null || altura > pool.getAlto()) {
            throw new IllegalArgumentException("Las lanes no caben en la altura del pool.");
        }
        nodos.listarVigentes(pool.getProceso().getId()).stream()
                .filter(n -> n.getPool().getId().equals(pool.getId()))
                .map(n -> Hibernate.unproxy(n, Nodo.class))
                .forEach(this::validarNodo);
    }

    public void ajustarLanes(Pool pool, Map<Long, Integer> anteriores) {
        // Al reordenar o cambiar alturas, la actividad sigue a su lane.
        for (Nodo registro : nodos.listarVigentes(pool.getProceso().getId())) {
            Nodo nodo = Hibernate.unproxy(registro, Nodo.class);
            if (nodo instanceof Actividad actividad && nodo.getPool().getId().equals(pool.getId())) {
                Lane lane = actividad.getLane();
                Integer anterior = anteriores.get(lane.getId());
                if (anterior != null) {
                    nodo.setPosicionY(trasladar(nodo.getPosicionY(), anterior, lane, nodo.getAlto()));
                }
            }
        }
        validarPool(pool);
    }
}
