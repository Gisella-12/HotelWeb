package com.hotel.hotelweb.controller;

import com.hotel.hotelweb.dao.ReservaDAO;
import com.hotel.hotelweb.dao.ClienteDAO;
import com.hotel.hotelweb.dao.HabitacionDAO;
import com.hotel.hotelweb.entity.Reserva;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class ReservaController {

    private static final Logger log =
            LoggerFactory.getLogger(ReservaController.class);

    private final ReservaDAO reservaDAO;
    private final ClienteDAO clienteDAO;
    private final HabitacionDAO habitacionDAO;

    public ReservaController(
            ReservaDAO reservaDAO,
            ClienteDAO clienteDAO,
            HabitacionDAO habitacionDAO) {

        this.reservaDAO = reservaDAO;
        this.clienteDAO = clienteDAO;
        this.habitacionDAO = habitacionDAO;
    }

    // LISTAR RESERVAS
    @GetMapping("/reservas")
    public String listarReservas(Model model) {

        model.addAttribute(
                "reservas",
                reservaDAO.listarReservas()
        );

        return "reservas";
    }

    // CARGAR LOS DATOS DEL FORMULARIO
    private void cargarFormulario(Model model, Reserva reserva) {

        model.addAttribute("reserva", reserva);

        model.addAttribute(
                "clientes",
                clienteDAO.listarClientes()
        );

        model.addAttribute(
                "habitaciones",
                habitacionDAO.listarHabitaciones()
        );
    }

    // FORMULARIO NUEVA RESERVA
    @GetMapping("/reservas/nueva")
    public String nuevaReserva(Model model) {

        cargarFormulario(model, new Reserva());

        return "formReserva";
    }

    // FORMULARIO EDITAR RESERVA
    @GetMapping("/reservas/editar/{id}")
    public String editarReserva(
            @PathVariable int id,
            Model model,
            RedirectAttributes atributos) {

        try {

            Reserva reserva = reservaDAO.buscarReserva(id);

            if (reserva.getFechaCheckIn() != null
                    || reserva.getFechaCheckOut() != null) {

                atributos.addFlashAttribute(
                        "error",
                        "No se puede editar una reserva "
                        + "que ya tiene check-in o check-out."
                );

                return "redirect:/reservas";
            }

            cargarFormulario(model, reserva);

            return "formReserva";

        } catch (EmptyResultDataAccessException e) {

            atributos.addFlashAttribute(
                    "error",
                    "La reserva que intentas editar no existe."
            );

        } catch (DataAccessException e) {

            log.error(
                    "Error al consultar la reserva {} para editar",
                    id,
                    e
            );

            atributos.addFlashAttribute(
                    "error",
                    "No se pudo cargar la reserva. "
                    + "Inténtalo nuevamente."
            );
        }

        return "redirect:/reservas";
    }

    // GUARDAR O ACTUALIZAR RESERVA
    @PostMapping("/reservas/guardar")
    public String guardarReserva(
            @ModelAttribute("reserva") Reserva reserva,
            BindingResult resultado,
            RedirectAttributes atributos) {

        if (resultado.hasErrors()) {

            atributos.addFlashAttribute(
                    "error",
                    "Revisa los datos ingresados. "
                    + "Las fechas y los identificadores "
                    + "deben tener un formato válido."
            );

            return "redirect:/reservas";
        }

        try {

            boolean nueva = reserva.getIdReserva() == 0;

            if (nueva) {
                reservaDAO.guardarReserva(reserva);
            } else {
                reservaDAO.actualizarReserva(reserva);
            }

            atributos.addFlashAttribute(
                    "mensaje",
                    nueva
                            ? "Reserva registrada correctamente."
                            : "Reserva actualizada correctamente."
            );

            log.info(
                    "Reserva guardada. Nueva: {}. ID: {}",
                    nueva,
                    reserva.getIdReserva()
            );

        } catch (IllegalStateException e) {

            atributos.addFlashAttribute(
                    "error",
                    e.getMessage()
            );

        } catch (DataAccessException e) {

            log.error(
                    "Error de base de datos al guardar la reserva",
                    e
            );

            atributos.addFlashAttribute(
                    "error",
                    "No se pudo guardar la reserva. "
                    + "Verifica que el cliente y la habitación "
                    + "existan e inténtalo nuevamente."
            );
        }

        return "redirect:/reservas";
    }

    // ELIMINAR RESERVA
    @GetMapping("/reservas/eliminar/{id}")
    public String eliminarReserva(
            @PathVariable int id,
            RedirectAttributes atributos) {

        try {

            Reserva reserva = reservaDAO.buscarReserva(id);

            if (reserva.getFechaCheckIn() != null
                    || reserva.getFechaCheckOut() != null) {

                atributos.addFlashAttribute(
                        "error",
                        "No se puede eliminar una reserva "
                        + "que ya tiene check-in o check-out."
                );

                return "redirect:/reservas";
            }

            reservaDAO.eliminarReserva(id);

            atributos.addFlashAttribute(
                    "mensaje",
                    "Reserva eliminada correctamente."
            );

            log.info("Reserva eliminada: {}", id);

        } catch (EmptyResultDataAccessException e) {

            atributos.addFlashAttribute(
                    "error",
                    "La reserva que intentas eliminar no existe."
            );

        } catch (DataAccessException e) {

            log.error(
                    "Error de base de datos al eliminar la reserva {}",
                    id,
                    e
            );

            atributos.addFlashAttribute(
                    "error",
                    "No se pudo eliminar la reserva. "
                    + "Puede tener pagos o consumos asociados. "
                    + "Revisa sus datos."
            );
        }

        return "redirect:/reservas";
    }

    // REGISTRAR CHECK-IN
    @PostMapping("/reservas/check-in/{id}")
    public String registrarCheckIn(
            @PathVariable int id,
            RedirectAttributes atributos) {

        try {

            reservaDAO.registrarCheckIn(id);

            atributos.addFlashAttribute(
                    "mensaje",
                    "Check-in registrado correctamente. "
                    + "La habitación ahora está ocupada."
            );

            log.info(
                    "Check-in registrado para la reserva {}",
                    id
            );

        } catch (IllegalStateException e) {

            atributos.addFlashAttribute(
                    "error",
                    e.getMessage()
            );

        } catch (DataAccessException e) {

            log.error(
                    "Error de base de datos al registrar "
                    + "check-in de la reserva {}",
                    id,
                    e
            );

            atributos.addFlashAttribute(
                    "error",
                    "No se pudo registrar el check-in. "
                    + "Revisa la conexión con la base de datos "
                    + "e inténtalo nuevamente."
            );
        }

        return "redirect:/reservas";
    }

    // REGISTRAR CHECK-OUT
    @PostMapping("/reservas/check-out/{id}")
    public String registrarCheckOut(
            @PathVariable int id,
            RedirectAttributes atributos) {

        try {

            reservaDAO.registrarCheckOut(id);

            atributos.addFlashAttribute(
                    "mensaje",
                    "Check-out registrado correctamente. "
                    + "La reserva está finalizada y "
                    + "la habitación pasó a Limpieza."
            );

            log.info(
                    "Check-out registrado para la reserva {}",
                    id
            );

        } catch (IllegalStateException e) {

            atributos.addFlashAttribute(
                    "error",
                    e.getMessage()
            );

        } catch (DataAccessException e) {

            log.error(
                    "Error de base de datos al registrar "
                    + "check-out de la reserva {}",
                    id,
                    e
            );

            atributos.addFlashAttribute(
                    "error",
                    "No se pudo registrar el check-out. "
                    + "Revisa la conexión con la base de datos "
                    + "e inténtalo nuevamente."
            );
        }

        return "redirect:/reservas";
    }
}