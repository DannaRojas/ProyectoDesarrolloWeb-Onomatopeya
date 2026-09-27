package com.proyecto.inicio.integracion.empresa;

import jakarta.persistence.EntityNotFoundException;
import org.springframework.context.annotation.Profile;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.LinkedHashMap;

@RestControllerAdvice(assignableTypes = ConexionEmpresaController.class)
@Profile("conexion-empresa")
public class ErroresConexionEmpresa {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail datosInvalidos(MethodArgumentNotValidException excepcion) {
        var respuesta = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Revisa los campos indicados.");
        var campos = new LinkedHashMap<String, String>();
        excepcion.getBindingResult().getFieldErrors()
                .forEach(error -> campos.putIfAbsent(error.getField(), error.getDefaultMessage()));
        respuesta.setProperty("campos", campos);
        return respuesta;
    }

    @ExceptionHandler(EntityNotFoundException.class)
    public ProblemDetail noEncontrada() {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, "Empresa no encontrada.");
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ProblemDetail reglaDeNegocio(IllegalArgumentException excepcion) {
        // Los mensajes de estas dos reglas pertenecen al servicio original.
        boolean duplicado = "Ya existe una empresa registrada con ese NIT".equals(excepcion.getMessage())
                || "Ya existe un usuario registrado con ese correo".equals(excepcion.getMessage());
        return ProblemDetail.forStatusAndDetail(
                duplicado ? HttpStatus.CONFLICT : HttpStatus.BAD_REQUEST,
                duplicado ? "El NIT o el correo del administrador ya está registrado." : "Los datos no son válidos.");
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ProblemDetail conflictoDeDatos() {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT,
                "Los datos entran en conflicto con los registros existentes.");
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class,
            HandlerMethodValidationException.class})
    public ProblemDetail peticionInvalida() {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Revisa el cuerpo y los parámetros de la petición.");
    }
}
