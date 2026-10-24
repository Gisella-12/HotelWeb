package com.hotel.hotelweb.controller;

import com.hotel.hotelweb.dao.ReporteDAO;
import com.hotel.hotelweb.entity.ReportePago;
import com.hotel.hotelweb.service.ReporteExcelService;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class ReporteController {

    private static final Logger log =
            LoggerFactory.getLogger(ReporteController.class);

    private final ReporteDAO reporteDAO;
    private final ReporteExcelService reporteExcelService;

    public ReporteController(
            ReporteDAO reporteDAO,
            ReporteExcelService reporteExcelService) {

        this.reporteDAO = reporteDAO;
        this.reporteExcelService = reporteExcelService;
    }

    // VALIDAR EL PERIODO DEL REPORTE
    private LocalDate[] validarFechas(
            String desde,
            String hasta) {

        if (desde == null || desde.isBlank()
                || hasta == null || hasta.isBlank()) {

            throw new IllegalStateException(
                    "Debes seleccionar las dos fechas."
            );
        }

        LocalDate fechaDesde;
        LocalDate fechaHasta;

        try {
            fechaDesde = LocalDate.parse(desde);
            fechaHasta = LocalDate.parse(hasta);

        } catch (DateTimeParseException e) {

            throw new IllegalStateException(
                    "Las fechas ingresadas no son válidas."
            );
        }

        if (fechaHasta.isBefore(fechaDesde)) {
            throw new IllegalStateException(
                    "La fecha final no puede ser anterior "
                    + "a la fecha inicial."
            );
        }

        return new LocalDate[]{
            fechaDesde,
            fechaHasta
        };
    }

    // MOSTRAR EL REPORTE DE PAGOS
    @GetMapping("/pagos/reportes")
    public String mostrarReporte(
            @RequestParam(name = "desde", required = false)
            String desde,
            @RequestParam(name = "hasta", required = false)
            String hasta,
            Model model) {

        // Al abrir por primera vez, consultar el mes actual.
        if (desde == null && hasta == null) {

            LocalDate hoy = LocalDate.now(
                    ZoneId.of("America/Lima")
            );

            desde = hoy.withDayOfMonth(1).toString();
            hasta = hoy.toString();
        }

        model.addAttribute("desde", desde);
        model.addAttribute("hasta", hasta);
        model.addAttribute("pagos", List.of());
        model.addAttribute("cantidadPagos", 0);
        model.addAttribute("totalRecaudado", BigDecimal.ZERO);
        model.addAttribute("consultaCorrecta", false);

        try {

            LocalDate[] fechas = validarFechas(desde, hasta);

            List<ReportePago> pagos =
                    reporteDAO.listarPagosPorFechas(
                            fechas[0],
                            fechas[1]
                    );

            BigDecimal total = pagos.stream()
                    .map(ReportePago::getMonto)
                    .reduce(
                            BigDecimal.ZERO,
                            BigDecimal::add
                    );

            model.addAttribute("pagos", pagos);
            model.addAttribute("cantidadPagos", pagos.size());
            model.addAttribute("totalRecaudado", total);
            model.addAttribute("consultaCorrecta", true);

        } catch (IllegalStateException e) {

            model.addAttribute(
                    "error",
                    e.getMessage()
            );

        } catch (DataAccessException e) {

            log.error(
                    "Error al consultar el reporte de pagos",
                    e
            );

            model.addAttribute(
                    "error",
                    "No se pudo consultar el reporte. "
                    + "Revisa la conexión con la base de datos."
            );
        }

        return "reportePagos";
    }

    // DESCARGAR EL REPORTE EN EXCEL
    @GetMapping("/pagos/reportes/excel")
    public String descargarExcel(
            @RequestParam(name = "desde", required = false)
            String desde,
            @RequestParam(name = "hasta", required = false)
            String hasta,
            HttpServletResponse respuesta,
            RedirectAttributes atributos) throws IOException {

        byte[] archivo;

        try {

            LocalDate[] fechas = validarFechas(desde, hasta);

            List<ReportePago> pagos =
                    reporteDAO.listarPagosPorFechas(
                            fechas[0],
                            fechas[1]
                    );

            archivo = reporteExcelService.generarReportePagos(
                    pagos,
                    fechas[0],
                    fechas[1]
            );

        } catch (IllegalStateException e) {

            atributos.addFlashAttribute(
                    "error",
                    e.getMessage()
            );

            return "redirect:/pagos/reportes";

        } catch (DataAccessException | IOException e) {

            log.error(
                    "Error al generar el reporte Excel",
                    e
            );

            atributos.addFlashAttribute(
                    "error",
                    "No se pudo generar el archivo Excel. "
                    + "Inténtalo nuevamente."
            );

            return "redirect:/pagos/reportes";
        }

        respuesta.setContentType(
                "application/vnd.openxmlformats-officedocument."
                + "spreadsheetml.sheet"
        );

        respuesta.setHeader(
                "Content-Disposition",
                "attachment; filename=\"reporte_pagos_"
                + desde + "_" + hasta + ".xlsx\""
        );

        respuesta.setHeader("Cache-Control", "no-store");
        respuesta.setContentLength(archivo.length);

        respuesta.getOutputStream().write(archivo);
        respuesta.getOutputStream().flush();

        log.info(
                "Reporte Excel de pagos descargado: {} a {}",
                desde,
                hasta
        );

        return null;
    }
}