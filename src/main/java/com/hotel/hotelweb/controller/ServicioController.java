package com.hotel.hotelweb.controller;

import com.hotel.hotelweb.dao.ServicioDAO;
import com.hotel.hotelweb.entity.Servicio;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
public class ServicioController {

    private final ServicioDAO servicioDAO;

    public ServicioController(ServicioDAO servicioDAO) {
        this.servicioDAO = servicioDAO;
    }

    // LISTAR SERVICIOS
    @GetMapping("/servicios")
    public String listarServicios(Model model) {

        model.addAttribute(
                "servicios",
                servicioDAO.listarServicios()
        );

        return "servicios";
    }

    // FORMULARIO NUEVO SERVICIO
    @GetMapping("/servicios/nuevo")
    public String nuevoServicio(Model model) {

        model.addAttribute(
                "servicio",
                new Servicio()
        );

        return "formServicio";
    }

    // FORMULARIO EDITAR SERVICIO
    @GetMapping("/servicios/editar/{id}")
    public String editarServicio(
            @PathVariable int id,
            Model model) {

        Servicio servicio =
                servicioDAO.buscarServicio(id);

        model.addAttribute(
                "servicio",
                servicio
        );

        return "formServicio";
    }

    // GUARDAR O ACTUALIZAR SERVICIO
    @PostMapping("/servicios/guardar")
    public String guardarServicio(
            @ModelAttribute Servicio servicio) {

        if (servicio.getIdServicio() == 0) {

            servicioDAO.guardarServicio(servicio);

        } else {

            servicioDAO.actualizarServicio(servicio);
        }

        return "redirect:/servicios";
    }

    // ELIMINAR SERVICIO
    @GetMapping("/servicios/eliminar/{id}")
    public String eliminarServicio(
            @PathVariable int id) {

        servicioDAO.eliminarServicio(id);

        return "redirect:/servicios";
    }
}