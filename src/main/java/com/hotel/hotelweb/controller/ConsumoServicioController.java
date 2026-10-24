package com.hotel.hotelweb.controller;

import com.google.common.collect.ImmutableSet;
import com.hotel.hotelweb.dao.ConsumoServicioDAO;
import com.hotel.hotelweb.dao.PagoDAO;
import com.hotel.hotelweb.dao.ReservaDAO;
import com.hotel.hotelweb.dao.ServicioDAO;
import com.hotel.hotelweb.entity.Reserva;
import com.hotel.hotelweb.entity.Servicio;
import java.math.BigDecimal;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class ConsumoServicioController {

    private static final Logger log =
            LoggerFactory.getLogger(ConsumoServicioController.class);

    // GUAVA: COLECCIÓN INMUTABLE DE ADICIONALES PERMITIDOS
    private static final ImmutableSet<String> ADICIONALES_PERMITIDOS =
            ImmutableSet.of(
                    "netflix",
                    "gaseosa",
                    "cerveza"
            );

    private final ConsumoServicioDAO consumoServicioDAO;
    private final ReservaDAO reservaDAO;
    private final ServicioDAO servicioDAO;
    private final PagoDAO pagoDAO;

    public ConsumoServicioController(
            ConsumoServicioDAO consumoServicioDAO,
            ReservaDAO reservaDAO,
            ServicioDAO servicioDAO,
            PagoDAO pagoDAO) {

        this.consumoServicioDAO = consumoServicioDAO;
        this.reservaDAO = reservaDAO;
        this.servicioDAO = servicioDAO;
        this.pagoDAO = pagoDAO;
    }

    // MOSTRAR ADICIONALES Y RESUMEN DE LA CUENTA
    @GetMapping("/reservas/{id}/consumos")
    public String mostrarConsumos(
            @PathVariable int id,
            Model model,
            RedirectAttributes atributos) {

        try {

            Reserva reserva = reservaDAO.buscarReserva(id);

            // Mostrar únicamente los tres adicionales acordados.
            List<Servicio> adicionales = servicioDAO.listarServicios()
                    .stream()
                    .filter(servicio -> esAdicionalPermitido(
                            servicio.getNombre()
                    ))
                    .toList();

            boolean permiteAgregar =
                    "En alojamiento".equals(reserva.getEstado())
                    && reserva.getFechaCheckIn() != null
                    && reserva.getFechaCheckOut() == null;

            BigDecimal totalConsumos =
                    consumoServicioDAO.totalPorReserva(id);

            BigDecimal totalPagado =
                    pagoDAO.totalPorReserva(id);

            model.addAttribute("reserva", reserva);
            model.addAttribute("adicionales", adicionales);

            model.addAttribute(
                    "consumos",
                    consumoServicioDAO.listarPorReserva(id)
            );

            model.addAttribute(
                    "pagos",
                    pagoDAO.listarPorReserva(id)
            );

            model.addAttribute("totalConsumos", totalConsumos);
            model.addAttribute("totalPagado", totalPagado);
            model.addAttribute("permiteAgregar", permiteAgregar);

            // Verificar si existen datos suficientes para calcular.
            boolean fechasValidas =
                    reserva.getFechaInicio() != null
                    && reserva.getFechaFin() != null
                    && reserva.getFechaFin().isAfter(
                            reserva.getFechaInicio()
                    );

            boolean tarifaValida =
                    reserva.getPrecioNoche() != null
                    && reserva.getPrecioNoche().signum() >= 0;

            boolean cuentaCalculable =
                    fechasValidas && tarifaValida;

            model.addAttribute(
                    "cuentaCalculable",
                    cuentaCalculable
            );

            if (cuentaCalculable) {

                long noches = ChronoUnit.DAYS.between(
                        reserva.getFechaInicio(),
                        reserva.getFechaFin()
                );

                BigDecimal totalAlojamiento =
                        reserva.getPrecioNoche().multiply(
                                BigDecimal.valueOf(noches)
                        );

                BigDecimal totalCuenta =
                        totalAlojamiento.add(totalConsumos);

                BigDecimal diferencia =
                        totalCuenta.subtract(totalPagado);

                // Si hay un pago mayor al total, se muestra aparte.
                BigDecimal saldoPendiente =
                        diferencia.max(BigDecimal.ZERO);

                BigDecimal saldoFavor =
                        diferencia.negate().max(BigDecimal.ZERO);

                model.addAttribute("noches", noches);

                model.addAttribute(
                        "totalAlojamiento",
                        totalAlojamiento
                );

                model.addAttribute(
                        "totalCuenta",
                        totalCuenta
                );

                model.addAttribute(
                        "saldoPendiente",
                        saldoPendiente
                );

                model.addAttribute(
                        "saldoFavor",
                        saldoFavor
                );

            } else {

                model.addAttribute(
                        "avisoCuenta",
                        "No se puede calcular la cuenta completa: "
                        + "revisa las fechas y la tarifa por noche "
                        + "guardada en esta reserva."
                );
            }

            return "consumos";

        } catch (EmptyResultDataAccessException e) {

            atributos.addFlashAttribute(
                    "error",
                    "La reserva no existe."
            );

        } catch (DataAccessException e) {

            log.error(
                    "Error al consultar la cuenta de la reserva {}",
                    id,
                    e
            );

            atributos.addFlashAttribute(
                    "error",
                    "No se pudo cargar la cuenta de la reserva. "
                    + "Revisa la conexión con la base de datos "
                    + "e inténtalo nuevamente."
            );
        }

        return "redirect:/reservas";
    }

    // GUARDAR EL ADICIONAL SOLICITADO
    @PostMapping("/reservas/{id}/consumos/guardar")
    public String guardarConsumo(
            @PathVariable int id,
            @RequestParam int idServicio,
            @RequestParam int cantidad,
            RedirectAttributes atributos) {

        try {

            consumoServicioDAO.registrarConsumo(
                    id,
                    idServicio,
                    cantidad
            );

            atributos.addFlashAttribute(
                    "mensaje",
                    "Adicional registrado correctamente."
            );

            log.info(
                    "Adicional registrado para la reserva {}",
                    id
            );

        } catch (IllegalStateException e) {

            atributos.addFlashAttribute(
                    "error",
                    e.getMessage()
            );

        } catch (DataAccessException e) {

            log.error(
                    "Error al guardar un adicional "
                    + "para la reserva {}",
                    id,
                    e
            );

            atributos.addFlashAttribute(
                    "error",
                    "No se pudo guardar el adicional. "
                    + "Revisa la conexión con la base de datos."
            );
        }

        return "redirect:/reservas/" + id + "/consumos";
    }

    // IDENTIFICAR LOS TRES ADICIONALES HABILITADOS
    private boolean esAdicionalPermitido(String nombre) {

        // COMMONS: quitar espacios de los extremos y manejar null.
        String nombreLimpio = StringUtils.trimToEmpty(nombre)
                .toLowerCase(Locale.ROOT);

        // GUAVA: consultar la colección de adicionales permitidos.
        return ADICIONALES_PERMITIDOS.contains(nombreLimpio);
    }
}