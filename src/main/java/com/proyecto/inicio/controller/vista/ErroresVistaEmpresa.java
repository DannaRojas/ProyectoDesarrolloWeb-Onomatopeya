package com.proyecto.inicio.controller.vista;

import jakarta.persistence.EntityNotFoundException;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.server.ResponseStatusException;

@ControllerAdvice(assignableTypes = EmpresaVistaController.class)
@Profile("conexion-empresa")
public class ErroresVistaEmpresa {
    @ExceptionHandler(EntityNotFoundException.class)
    public ModelAndView noEncontrada() {
        return error(HttpStatus.NOT_FOUND, "Empresa no encontrada.");
    }

    @ExceptionHandler({MethodArgumentTypeMismatchException.class, HandlerMethodValidationException.class})
    public ModelAndView datosInvalidos() {
        return error(HttpStatus.BAD_REQUEST, "Revisa el identificador de la empresa.");
    }

    private ModelAndView error(HttpStatus estado, String mensaje) {
        ModelAndView vista = new ModelAndView("empresas/error");
        vista.setStatus(estado);
        vista.addObject("mensajeError", mensaje);
        return vista;
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ModelAndView formularioNoValido(ResponseStatusException excepcion) {
        return error(HttpStatus.valueOf(excepcion.getStatusCode().value()),
                "Vuelve a abrir el formulario e intenta de nuevo.");
    }
}
