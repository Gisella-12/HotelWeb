package com.hotel.hotelweb.controller;

import com.hotel.hotelweb.dao.UsuarioDAO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class LoginController {

    private static final Logger log =
            LoggerFactory.getLogger(LoginController.class);

    private final UsuarioDAO usuarioDAO;

    public LoginController(UsuarioDAO usuarioDAO) {
        this.usuarioDAO = usuarioDAO;
    }

    // MOSTRAR LOGIN
    @GetMapping({"/", "/login"})
    public String mostrarLogin(HttpServletRequest request) {

        HttpSession session = request.getSession(false);

        if (session != null
                && session.getAttribute("usuarioLogueado") != null) {

            return "redirect:/panel";
        }

        return "login";
    }

    // PROCESAR LOGIN
    @PostMapping("/login")
    public String procesarLogin(
            @RequestParam(name = "usuario", defaultValue = "")
            String usuario,
            @RequestParam(name = "password", defaultValue = "")
            String password,
            HttpServletRequest request,
            Model model) {

        String nombreUsuario = usuario.trim();

        if (nombreUsuario.isBlank() || password.isBlank()) {

            model.addAttribute(
                    "error",
                    "Debes ingresar tu usuario y contraseña."
            );

            return "login";
        }

        try {

            boolean credencialesValidas =
                    usuarioDAO.validarUsuario(
                            nombreUsuario,
                            password
                    );

            if (!credencialesValidas) {

                model.addAttribute(
                        "error",
                        "Usuario o contraseña incorrectos."
                );

                log.warn("Intento de inicio de sesión rechazado.");

                return "login";
            }

            // CERRAR LA SESIÓN ANTERIOR, SI EXISTE
            HttpSession sesionAnterior =
                    request.getSession(false);

            if (sesionAnterior != null) {
                sesionAnterior.invalidate();
            }

            // CREAR UNA NUEVA SESIÓN AUTENTICADA
            HttpSession nuevaSesion = request.getSession(true);

            nuevaSesion.setAttribute(
                    "usuarioLogueado",
                    nombreUsuario
            );

            // EXPIRAR TRAS 30 MINUTOS SIN ACTIVIDAD
            nuevaSesion.setMaxInactiveInterval(30 * 60);

            log.info("Inicio de sesión correcto.");

            return "redirect:/panel";

        } catch (DataAccessException e) {

            log.error(
                    "Error de base de datos al iniciar sesión.",
                    e
            );

            model.addAttribute(
                    "error",
                    "No se pudo iniciar sesión. "
                    + "Revisa la conexión con la base de datos "
                    + "e inténtalo nuevamente."
            );

            return "login";
        }
    }

    // CERRAR SESIÓN
    @GetMapping("/logout")
    public String cerrarSesion(HttpServletRequest request) {

        HttpSession session = request.getSession(false);

        if (session != null) {
            session.invalidate();
        }

        log.info("Sesión cerrada.");

        return "redirect:/login";
    }
}