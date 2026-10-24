package com.hotel.hotelweb.controller;

import com.hotel.hotelweb.dao.DashboardDAO;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class PanelController {

    private final DashboardDAO dashboardDAO;

    public PanelController(DashboardDAO dashboardDAO) {
        this.dashboardDAO = dashboardDAO;
    }

    @GetMapping("/panel")
    public String mostrarPanel(
            Model model,
            HttpSession session) {

        // VERIFICAR SI EL USUARIO INICIO SESION
        if (session.getAttribute("usuarioLogueado") == null) {
            return "redirect:/login";
        }

        model.addAttribute(
                "totalHabitaciones",
                dashboardDAO.totalHabitaciones()
        );

        model.addAttribute(
                "disponibles",
                dashboardDAO.habitacionesDisponibles()
        );

        model.addAttribute(
                "ocupadas",
                dashboardDAO.habitacionesOcupadas()
        );

        model.addAttribute(
                "mantenimiento",
                dashboardDAO.habitacionesMantenimiento()
        );

        model.addAttribute(
                "totalClientes",
                dashboardDAO.totalClientes()
        );

        model.addAttribute(
                "totalReservas",
                dashboardDAO.totalReservas()
        );

        model.addAttribute(
                "totalIngresos",
                dashboardDAO.totalIngresos()
        );

        model.addAttribute(
                "habitaciones",
                dashboardDAO.listarHabitacionesPanel()
        );

        return "panel";
    }
}