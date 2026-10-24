package com.hotel.hotelweb.controller;

import com.hotel.hotelweb.dao.PagoDAO;
import com.hotel.hotelweb.dao.ReservaDAO;
import com.hotel.hotelweb.entity.Pago;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
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
public class PagoController {

    private static final Logger log =
            LoggerFactory.getLogger(PagoController.class);

    private final PagoDAO pagoDAO;
    private final ReservaDAO reservaDAO;

    public PagoController(
            PagoDAO pagoDAO,
            ReservaDAO reservaDAO) {

        this.pagoDAO = pagoDAO;
        this.reservaDAO = reservaDAO;
    }

    // LISTAR PAGOS
    @GetMapping("/pagos")
    public String listarPagos(Model model) {

        model.addAttribute(
                "pagos",
                pagoDAO.listarPagos()
        );

        return "pagos";
    }

    // CARGAR LOS DATOS DEL FORMULARIO
    private void cargarFormulario(Model model, Pago pago) {

        model.addAttribute("pago", pago);

        model.addAttribute(
                "reservas",
                reservaDAO.listarReservas()
        );
    }

    // FORMULARIO NUEVO PAGO
    @GetMapping("/pagos/nuevo")
    public String nuevoPago(Model model) {

        Pago pago = new Pago();

        pago.setFechaPago(
                LocalDate.now(ZoneId.of("America/Lima"))
        );

        cargarFormulario(model, pago);

        return "formPago";
    }

    // FORMULARIO EDITAR PAGO
    @GetMapping("/pagos/editar/{id}")
    public String editarPago(
            @PathVariable int id,
            Model model,
            RedirectAttributes atributos) {

        try {

            Pago pago = pagoDAO.buscarPago(id);

            cargarFormulario(model, pago);

            return "formPago";

        } catch (EmptyResultDataAccessException e) {

            atributos.addFlashAttribute(
                    "error",
                    "El pago que intentas editar no existe."
            );

        } catch (DataAccessException e) {

            log.error(
                    "Error al cargar el pago {}",
                    id,
                    e
            );

            atributos.addFlashAttribute(
                    "error",
                    "No se pudo cargar el pago. "
                    + "Inténtalo nuevamente."
            );
        }

        return "redirect:/pagos";
    }

    // VALIDAR LOS DATOS DEL PAGO
    private void validarPago(Pago pago) {

        if (pago.getIdPago() < 0) {
            throw new IllegalStateException(
                    "El identificador del pago no es válido."
            );
        }

        if (pago.getIdReserva() <= 0) {
            throw new IllegalStateException(
                    "Debes seleccionar una reserva."
            );
        }

        if (pago.getFechaPago() == null) {
            throw new IllegalStateException(
                    "Debes ingresar la fecha del pago."
            );
        }

        LocalDate hoy = LocalDate.now(
                ZoneId.of("America/Lima")
        );

        if (pago.getFechaPago().isAfter(hoy)) {
            throw new IllegalStateException(
                    "La fecha del pago no puede ser futura."
            );
        }

        if (!Double.isFinite(pago.getMonto())
                || pago.getMonto() <= 0) {

            throw new IllegalStateException(
                    "El monto del pago debe ser mayor que cero."
            );
        }

        BigDecimal monto = BigDecimal.valueOf(
                pago.getMonto()
        );

        if (monto.stripTrailingZeros().scale() > 2) {
            throw new IllegalStateException(
                    "El monto debe tener como máximo "
                    + "dos decimales."
            );
        }

        if (pago.getMetodoPago() == null
                || pago.getMetodoPago().isBlank()) {

            throw new IllegalStateException(
                    "Debes seleccionar un método de pago."
            );
        }

        pago.setMetodoPago(
                pago.getMetodoPago().trim()
        );
    }

    // GUARDAR O ACTUALIZAR PAGO
    @PostMapping("/pagos/guardar")
    public String guardarPago(
            @ModelAttribute("pago") Pago pago,
            BindingResult resultado,
            RedirectAttributes atributos) {

        if (resultado.hasErrors()) {

            atributos.addFlashAttribute(
                    "error",
                    "Revisa los datos del pago. "
                    + "La fecha y el monto deben "
                    + "tener un formato válido."
            );

            return "redirect:/pagos";
        }

        try {

            validarPago(pago);

            boolean nuevo = pago.getIdPago() == 0;

            if (nuevo) {
                pagoDAO.guardarPago(pago);
            } else {
                pagoDAO.actualizarPago(pago);
            }

            atributos.addFlashAttribute(
                    "mensaje",
                    nuevo
                            ? "Pago registrado correctamente."
                            : "Pago actualizado correctamente."
            );

            log.info(
                    "Pago guardado para la reserva {}. Nuevo: {}",
                    pago.getIdReserva(),
                    nuevo
            );

        } catch (IllegalStateException e) {

            atributos.addFlashAttribute(
                    "error",
                    e.getMessage()
            );

        } catch (DataAccessException e) {

            log.error(
                    "Error al guardar un pago de la reserva {}",
                    pago.getIdReserva(),
                    e
            );

            atributos.addFlashAttribute(
                    "error",
                    "No se pudo guardar el pago. "
                    + "Verifica la reserva y la conexión "
                    + "con la base de datos."
            );
        }

        return "redirect:/pagos";
    }

    // ELIMINAR PAGO
    @GetMapping("/pagos/eliminar/{id}")
    public String eliminarPago(
            @PathVariable int id,
            RedirectAttributes atributos) {

        try {

            pagoDAO.eliminarPago(id);

            atributos.addFlashAttribute(
                    "mensaje",
                    "Pago eliminado correctamente."
            );

            log.info("Pago eliminado: {}", id);

        } catch (IllegalStateException e) {

            atributos.addFlashAttribute(
                    "error",
                    e.getMessage()
            );

        } catch (DataAccessException e) {

            log.error(
                    "Error al eliminar el pago {}",
                    id,
                    e
            );

            atributos.addFlashAttribute(
                    "error",
                    "No se pudo eliminar el pago. "
                    + "Inténtalo nuevamente."
            );
        }

        return "redirect:/pagos";
    }
}