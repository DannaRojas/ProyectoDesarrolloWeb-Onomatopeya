package com.proyecto.inicio.service;

import com.proyecto.inicio.entity.*;
import com.proyecto.inicio.entity.enums.EstadoNodo;
import com.proyecto.inicio.repository.*;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class EntregaNodosArcosTests {
    private final NodoRepository nodoRepository = mock(NodoRepository.class);
    private final ArcoRepository arcoRepository = mock(ArcoRepository.class);
    private final ArcoService arcoService = new ArcoService(nodoRepository, arcoRepository);

    @Test
    void creaArcoValidoDentroDelMismoPool() {
        Proceso proceso = proceso(1L);
        Pool pool = pool(10L, proceso);
        Nodo origen = nodo(pool, proceso);
        Nodo destino = nodo(pool, proceso);
        ReflectionTestUtils.setField(origen, "id", 100L);
        ReflectionTestUtils.setField(destino, "id", 101L);
        when(nodoRepository.findById(100L)).thenReturn(java.util.Optional.of(origen));
        when(nodoRepository.findById(101L)).thenReturn(java.util.Optional.of(destino));
        when(arcoRepository.save(any(Arco.class))).thenAnswer(inv -> inv.getArgument(0));

        Arco arco = arcoService.crear(100L, 101L, "continuar", null);

        assertSame(origen, arco.getOrigen());
        assertSame(destino, arco.getDestino());
        assertTrue(arco.getActivo());
        verify(arcoRepository).save(any(Arco.class));
    }

    @Test
    void rechazaArcoEntrePoolsDistintos() {
        Proceso proceso = proceso(1L);
        Nodo origen = nodo(pool(10L, proceso), proceso);
        Nodo destino = nodo(pool(11L, proceso), proceso);
        ReflectionTestUtils.setField(origen, "id", 100L);
        ReflectionTestUtils.setField(destino, "id", 101L);
        when(nodoRepository.findById(100L)).thenReturn(java.util.Optional.of(origen));
        when(nodoRepository.findById(101L)).thenReturn(java.util.Optional.of(destino));

        assertThrows(IllegalArgumentException.class, () -> arcoService.crear(100L, 101L, null, null));
        verify(arcoRepository, never()).save(any(Arco.class));
    }

    @Test
    void rechazaActividadSinLane() {
        ActividadRepository actividadRepository = mock(ActividadRepository.class);
        NodoService nodoService = new NodoService(actividadRepository, nodoRepository, arcoRepository);
        Actividad actividad = new Actividad();
        actividad.setProceso(proceso(1L));
        actividad.setPool(pool(10L, actividad.getProceso()));
        actividad.setNombre("Validar solicitud");
        actividad.setTipo(com.proyecto.inicio.entity.enums.TipoActividad.TAREA);

        assertThrows(IllegalArgumentException.class, () -> nodoService.guardarActividad(actividad));
        verify(actividadRepository, never()).save(any(Actividad.class));
    }

    @Test
    void guardaGatewayIncompletoComoBorrador() {
        NodoService nodoService = new NodoService(mock(ActividadRepository.class), nodoRepository, arcoRepository);
        Gateway gateway = new Gateway();
        gateway.setProceso(proceso(1L));
        gateway.setPool(pool(10L, gateway.getProceso()));
        when(nodoRepository.save(gateway)).thenReturn(gateway);

        Gateway guardado = nodoService.guardarGatewayBorrador(gateway);

        assertNull(guardado.getTipo());
        assertNull(guardado.getDireccion());
        assertEquals(EstadoNodo.BORRADOR, guardado.getEstado());
        verify(nodoRepository).save(gateway);
    }

    private static Proceso proceso(Long id) {
        Proceso proceso = new Proceso();
        ReflectionTestUtils.setField(proceso, "id", id);
        return proceso;
    }

    private static Pool pool(Long id, Proceso proceso) {
        Pool pool = new Pool();
        ReflectionTestUtils.setField(pool, "id", id);
        pool.setProceso(proceso);
        return pool;
    }

    private static Nodo nodo(Pool pool, Proceso proceso) {
        Actividad nodo = new Actividad();
        nodo.setPool(pool);
        nodo.setProceso(proceso);
        nodo.setNombre("Nodo");
        return nodo;
    }
}
