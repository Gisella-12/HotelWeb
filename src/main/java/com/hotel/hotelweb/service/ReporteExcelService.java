package com.hotel.hotelweb.service;

import com.hotel.hotelweb.entity.ReportePago;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

@Service
public class ReporteExcelService {

    public byte[] generarReportePagos(
            List<ReportePago> pagos,
            LocalDate desde,
            LocalDate hasta) throws IOException {

        try (XSSFWorkbook libro = new XSSFWorkbook();
             ByteArrayOutputStream salida =
                     new ByteArrayOutputStream()) {

            Sheet hoja = libro.createSheet("Pagos");

            DateTimeFormatter formatoFecha =
                    DateTimeFormatter.ofPattern("dd/MM/yyyy");

            // ESTILO DE LOS ENCABEZADOS
            Font fuenteEncabezado = libro.createFont();
            fuenteEncabezado.setBold(true);
            fuenteEncabezado.setColor(
                    IndexedColors.WHITE.getIndex()
            );

            CellStyle estiloEncabezado =
                    libro.createCellStyle();

            estiloEncabezado.setFont(fuenteEncabezado);
            estiloEncabezado.setFillForegroundColor(
                    IndexedColors.DARK_BLUE.getIndex()
            );
            estiloEncabezado.setFillPattern(
                    FillPatternType.SOLID_FOREGROUND
            );

            // ESTILO DE LOS IMPORTES
            CellStyle estiloMonto = libro.createCellStyle();

            estiloMonto.setDataFormat(
                    libro.createDataFormat().getFormat(
                            "\"S/ \"#,##0.00"
                    )
            );

            Font fuenteNegrita = libro.createFont();
            fuenteNegrita.setBold(true);

            CellStyle estiloTotal = libro.createCellStyle();
            estiloTotal.cloneStyleFrom(estiloMonto);
            estiloTotal.setFont(fuenteNegrita);

            // TÍTULO Y PERIODO
            Row titulo = hoja.createRow(0);
            titulo.createCell(0).setCellValue(
                    "HotelWeb - Reporte de pagos"
            );
            hoja.addMergedRegion(
                    new CellRangeAddress(0, 0, 0, 6)
            );

            Row periodo = hoja.createRow(1);
            periodo.createCell(0).setCellValue(
                    "Desde: " + desde.format(formatoFecha)
                    + " hasta: " + hasta.format(formatoFecha)
            );
            hoja.addMergedRegion(
                    new CellRangeAddress(1, 1, 0, 6)
            );

            Row descripcion = hoja.createRow(2);
            descripcion.createCell(0).setCellValue(
                    "Pagos registrados según su fecha de pago."
            );
            hoja.addMergedRegion(
                    new CellRangeAddress(2, 2, 0, 6)
            );

            // ENCABEZADOS
            String[] columnas = {
                "ID del pago",
                "Reserva",
                "Cliente",
                "Habitación",
                "Fecha de pago",
                "Método de pago",
                "Monto"
            };

            Row encabezado = hoja.createRow(4);

            for (int i = 0; i < columnas.length; i++) {
                encabezado.createCell(i).setCellValue(columnas[i]);
                encabezado.getCell(i).setCellStyle(
                        estiloEncabezado
                );
            }

            // DETALLE DE PAGOS
            int numeroFila = 5;
            BigDecimal total = BigDecimal.ZERO;

            for (ReportePago pago : pagos) {

                Row fila = hoja.createRow(numeroFila++);

                fila.createCell(0).setCellValue(
                        pago.getIdPago()
                );
                fila.createCell(1).setCellValue(
                        pago.getIdReserva()
                );
                fila.createCell(2).setCellValue(
                        pago.getNombreCliente()
                );
                fila.createCell(3).setCellValue(
                        pago.getDescripcionHabitacion()
                );
                fila.createCell(4).setCellValue(
                        pago.getFechaPago().format(formatoFecha)
                );
                fila.createCell(5).setCellValue(
                        pago.getMetodoPago()
                );

                fila.createCell(6).setCellValue(
                        pago.getMonto().doubleValue()
                );
                fila.getCell(6).setCellStyle(estiloMonto);

                total = total.add(pago.getMonto());
            }

            // TOTAL DEL REPORTE
            Row filaTotal = hoja.createRow(numeroFila + 1);

            filaTotal.createCell(5).setCellValue(
                    "Total recaudado"
            );
            filaTotal.createCell(6).setCellValue(
                    total.doubleValue()
            );
            filaTotal.getCell(6).setCellStyle(estiloTotal);

            Row cantidad = hoja.createRow(numeroFila + 3);
            cantidad.createCell(0).setCellValue(
                    "Cantidad de pagos: " + pagos.size()
            );

            // ANCHOS DE COLUMNAS
            hoja.setColumnWidth(0, 15 * 256);
            hoja.setColumnWidth(1, 12 * 256);
            hoja.setColumnWidth(2, 30 * 256);
            hoja.setColumnWidth(3, 24 * 256);
            hoja.setColumnWidth(4, 18 * 256);
            hoja.setColumnWidth(5, 22 * 256);
            hoja.setColumnWidth(6, 20 * 256);

            hoja.createFreezePane(0, 5);

            hoja.setAutoFilter(
                    new CellRangeAddress(
                            4,
                            Math.max(4, numeroFila - 1),
                            0,
                            6
                    )
            );

            libro.write(salida);

            return salida.toByteArray();
        }
    }
}