package com.proyecto.inicio.exception;

import com.proyecto.inicio.controller.*;
import jakarta.servlet.http.HttpServletRequest;

import com.proyecto.inicio.exception.AccesoColaboracionException;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.ConstraintViolationException;
import org.springframework.context.annotation.Profile;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.validation.method.ParameterErrors;
import java.util.LinkedHashMap;

@RestControllerAdvice(assignableTypes = {EmpresaController.class, MensajeController.class,
        FlujoMensajeController.class, PermisoEstructuraController.class, UsuarioController.class})
@Profile("conexion-empresa")
public class ErroresApi {
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail validacion(MethodArgumentNotValidException excepcion) {
        var respuesta = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Revisa los campos indicados.");
        var campos = new LinkedHashMap<String, String>();
        excepcion.getBindingResult().getFieldErrors()
                .forEach(error -> campos.putIfAbsent(error.getField(), error.getDefaultMessage()));
        respuesta.setProperty("campos", campos);
        return respuesta;
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    public ProblemDetail validacionMetodo(HandlerMethodValidationException excepcion) {
        var respuesta = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Revisa los campos y parámetros indicados.");
        var campos = new LinkedHashMap<String, String>();
        excepcion.getParameterValidationResults().forEach(resultado -> {
            if (resultado instanceof ParameterErrors errores)
                errores.getFieldErrors().forEach(error -> campos.putIfAbsent(error.getField(), error.getDefaultMessage()));
            else resultado.getResolvableErrors().forEach(error ->
                    campos.putIfAbsent(String.valueOf(resultado.getMethodParameter().getParameterName()), error.getDefaultMessage()));
        });
        respuesta.setProperty("campos", campos);
        return respuesta;
    }

    @ExceptionHandler({ConstraintViolationException.class, HttpMessageNotReadableException.class,
            MethodArgumentTypeMismatchException.class})
    public ProblemDetail peticionInvalida() {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Revisa los datos y parámetros de la petición.");
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ProblemDetail reglaInvalida(IllegalArgumentException excepcion, HttpServletRequest peticion) {
        if (peticion.getRequestURI().startsWith("/empresas")) {
            boolean duplicado = "Ya existe una empresa registrada con ese NIT".equals(excepcion.getMessage())
                    || "Ya existe un usuario registrado con ese correo".equals(excepcion.getMessage());
            return ProblemDetail.forStatusAndDetail(duplicado ? HttpStatus.CONFLICT : HttpStatus.BAD_REQUEST,
                    duplicado ? "El NIT o el correo del administrador ya está registrado." : "Los datos no son válidos.");
        }
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, excepcion.getMessage());
    }

    @ExceptionHandler(EntityNotFoundException.class)
    public ProblemDetail noEncontrado(HttpServletRequest peticion) {
        String detalle = peticion.getRequestURI().startsWith("/empresas")
                ? "Empresa no encontrada." : "No se encontró el registro en este proceso.";
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, detalle);
    }

    @ExceptionHandler(AccesoColaboracionException.class)
    public ProblemDetail accesoDenegado() {
        return ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, "No tienes permiso para esta operación.");
    }

    @ExceptionHandler(IllegalStateException.class)
    public ProblemDetail conflicto(IllegalStateException excepcion) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, excepcion.getMessage());
    }

    @ExceptionHandler({DataIntegrityViolationException.class, OptimisticLockingFailureException.class})
    public ProblemDetail conflictoDatos() {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, "El registro cambió o entra en conflicto con los datos existentes.");
    }
}
