package com.hotel.hotelweb.controller;

import com.hotel.hotelweb.dao.HabitacionDAO;
import com.hotel.hotelweb.entity.Habitacion;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
public class HabitacionController {

    private final HabitacionDAO habitacionDAO;

    public HabitacionController(HabitacionDAO habitacionDAO) {
        this.habitacionDAO = habitacionDAO;
    }

    // LISTAR HABITACIONES
    @GetMapping("/habitaciones")
    public String listarHabitaciones(Model model) {

        model.addAttribute(
                "habitaciones",
                habitacionDAO.listarHabitaciones()
        );

        return "habitaciones";
    }

    // MOSTRAR FORMULARIO NUEVA HABITACION
    @GetMapping("/habitaciones/nueva")
    public String nuevaHabitacion(Model model) {

        model.addAttribute(
                "habitacion",
                new Habitacion()
        );

        return "formHabitacion";
    }

    // MOSTRAR FORMULARIO PARA EDITAR
    @GetMapping("/habitaciones/editar/{id}")
    public String editarHabitacion(
            @PathVariable int id,
            Model model) {

        Habitacion habitacion =
                habitacionDAO.buscarPorId(id);

        model.addAttribute(
                "habitacion",
                habitacion
        );

        return "formHabitacion";
    }

    // GUARDAR O ACTUALIZAR HABITACION
    @PostMapping("/habitaciones/guardar")
    public String guardarHabitacion(
            @ModelAttribute Habitacion habitacion) {

        if (habitacion.getIdHabitacion() == 0) {

            habitacionDAO.guardarHabitacion(habitacion);

        } else {

            habitacionDAO.actualizarHabitacion(habitacion);

        }

        return "redirect:/habitaciones";
    }

    // ELIMINAR HABITACION
    @GetMapping("/habitaciones/eliminar/{id}")
    public String eliminarHabitacion(
            @PathVariable int id) {

        habitacionDAO.eliminarHabitacion(id);

        return "redirect:/habitaciones";
    }
}