package com.proyecto.inicio.exception;

public class AccesoColaboracionException extends RuntimeException {
    public AccesoColaboracionException() {
        super("No tienes permiso para realizar esta operación.");
    }
}
