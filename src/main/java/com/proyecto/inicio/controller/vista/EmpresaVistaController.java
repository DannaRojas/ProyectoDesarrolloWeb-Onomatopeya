package com.proyecto.inicio.controller.vista;

import com.proyecto.inicio.service.EmpresaService;
import com.proyecto.inicio.service.EmpresaVistaService;
import com.proyecto.inicio.dto.formulario.EmpresaFormulario;
import com.proyecto.inicio.dto.formulario.RegistroEmpresaFormulario;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@Profile("conexion-empresa")
@RequestMapping("/vista/empresas")
@RequiredArgsConstructor
public class EmpresaVistaController {
    private final EmpresaService empresas;
    private final EmpresaVistaService formularios;
    private final TokenFormularioEmpresa tokens;

    @ModelAttribute("tokenFormulario")
    public String token(HttpServletRequest request) {
        return tokens.obtener(request.getSession());
    }

    @InitBinder({"registro", "formulario"})
    public void camposPermitidos(WebDataBinder binder) {
        binder.setAllowedFields("nombre", "nit", "correoContacto", "nombreAdmin", "correoAdmin", "contrasenaAdmin");
    }

    @GetMapping("/nueva")
    public String nuevo(Model model) {
        model.addAttribute("registro", new RegistroEmpresaFormulario());
        return "empresas/registro";
    }

    @PostMapping
    public String registrar(@Valid @ModelAttribute("registro") RegistroEmpresaFormulario registro, BindingResult errores,
            @RequestParam(name = "_token", required = false) String token, HttpServletRequest request,
            HttpServletResponse response, RedirectAttributes redireccion) {
        tokens.validar(request.getSession(false), token);
        if (!errores.hasErrors()) {
            try {
                var empresa = formularios.registrar(registro);
                redireccion.addFlashAttribute("mensaje", "Empresa registrada.");
                return "redirect:/vista/empresas/" + empresa.getId();
            } catch (IllegalArgumentException excepcion) {
                errorDeNegocio(errores, excepcion, true, response);
            } catch (DataIntegrityViolationException excepcion) {
                errores.reject("duplicado", "El NIT o el correo del administrador ya están registrados.");
                response.setStatus(409);
            }
        } else response.setStatus(400);
        // No devolver la contraseña al navegador cuando se vuelve a mostrar el formulario.
        registro.setContrasenaAdmin(null);
        return "empresas/registro";
    }

    @GetMapping("/{id}/editar")
    public String editar(@PathVariable @Positive Long id, Model model) {
        model.addAttribute("empresaId", id);
        model.addAttribute("formulario", formularios.prepararEdicion(id));
        return "empresas/editar";
    }

    @PostMapping("/{id}/editar")
    public String actualizar(@PathVariable @Positive Long id,
            @Valid @ModelAttribute("formulario") EmpresaFormulario formulario, BindingResult errores,
            @RequestParam(name = "_token", required = false) String token, HttpServletRequest request,
            HttpServletResponse response, Model model, RedirectAttributes redireccion) {
        tokens.validar(request.getSession(false), token);
        model.addAttribute("empresaId", id);
        if (!errores.hasErrors()) {
            try {
                formularios.actualizar(id, formulario);
                redireccion.addFlashAttribute("mensaje", "Cambios guardados.");
                return "redirect:/vista/empresas/" + id;
            } catch (IllegalArgumentException excepcion) {
                errorDeNegocio(errores, excepcion, false, response);
            } catch (DataIntegrityViolationException excepcion) {
                errores.rejectValue("nit", "duplicado", "El NIT ya está registrado.");
                response.setStatus(409);
            }
        } else response.setStatus(400);
        return "empresas/editar";
    }

    private void errorDeNegocio(BindingResult errores, IllegalArgumentException excepcion,
            boolean registro, HttpServletResponse response) {
        if ("Ya existe una empresa registrada con ese NIT".equals(excepcion.getMessage())) {
            errores.rejectValue("nit", "duplicado", "El NIT ya está registrado.");
            response.setStatus(409);
        } else if (registro && "Ya existe un usuario registrado con ese correo".equals(excepcion.getMessage())) {
            errores.rejectValue("correoAdmin", "duplicado", "El correo del administrador ya está registrado.");
            response.setStatus(409);
        } else {
            errores.reject("datos", "Revisa los datos de la empresa.");
            response.setStatus(400);
        }
    }

    @GetMapping
    public String listar(Model model) {
        model.addAttribute("empresas", empresas.consultarTodas());
        return "empresas/lista";
    }

    @GetMapping("/{id}")
    public String detalle(@PathVariable @Positive Long id, Model model) {
        model.addAttribute("empresa", empresas.consultarPorId(id));
        return "empresas/detalle";
    }
}
