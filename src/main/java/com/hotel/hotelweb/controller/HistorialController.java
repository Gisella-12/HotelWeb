package com.hotel.hotelweb.controller;

import com.hotel.hotelweb.dao.ReservaDAO;
import com.hotel.hotelweb.entity.Reserva;
import java.util.Comparator;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class HistorialController {

    private static final Logger log =
            LoggerFactory.getLogger(HistorialController.class);

    private final ReservaDAO reservaDAO;

    public HistorialController(ReservaDAO reservaDAO) {
        this.reservaDAO = reservaDAO;
    }

    // CONSULTAR ESTANCIAS CON SALIDA REGISTRADA
    @GetMapping("/reservas/historial")
    public String mostrarHistorial(
            Model model,
            RedirectAttributes atributos) {

        try {

            List<Reserva> historial = reservaDAO.listarReservas()
                    .stream()
                    .filter(reserva ->
                            reserva.getFechaCheckOut() != null
                    )
                    .sorted(
                            Comparator.comparing(
                                    Reserva::getFechaCheckOut
                            )
                            .reversed()
                            .thenComparing(
                                    Comparator.comparingInt(
                                            Reserva::getIdReserva
                                    ).reversed()
                            )
                    )
                    .toList();

            model.addAttribute("historial", historial);

            model.addAttribute(
                    "totalEstancias",
                    historial.size()
            );

            return "historial";

        } catch (DataAccessException e) {

            log.error(
                    "Error al consultar el historial de reservas",
                    e
            );

            atributos.addFlashAttribute(
                    "error",
                    "No se pudo cargar el historial. "
                    + "Revisa la conexión con la base de datos "
                    + "e inténtalo nuevamente."
            );

            return "redirect:/reservas";
        }
    }
}