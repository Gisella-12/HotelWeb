package com.hotel.hotelweb.dao;

import com.hotel.hotelweb.entity.ReportePago;
import java.time.LocalDate;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class ReporteDAO {

    private final JdbcTemplate jdbcTemplate;

    public ReporteDAO(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    // CONSULTAR PAGOS ENTRE DOS FECHAS, AMBAS INCLUIDAS
    public List<ReportePago> listarPagosPorFechas(
            LocalDate desde,
            LocalDate hasta) {

        if (desde == null || hasta == null) {
            throw new IllegalStateException(
                    "Debes seleccionar las dos fechas."
            );
        }

        if (hasta.isBefore(desde)) {
            throw new IllegalStateException(
                    "La fecha final no puede ser anterior "
                    + "a la fecha inicial."
            );
        }

        String sql = """
                SELECT
                    p.id_pago,
                    p.id_reserva,
                    p.fecha_pago,
                    p.monto,
                    p.metodo_pago,
                    CONCAT(c.nombre, ' ', c.apellido)
                        AS nombre_cliente,
                    CONCAT(h.numero, ' - ', h.tipo)
                        AS descripcion_habitacion
                FROM pagos p
                INNER JOIN reservas r
                    ON p.id_reserva = r.id_reserva
                INNER JOIN clientes c
                    ON r.id_cliente = c.id_cliente
                INNER JOIN habitaciones h
                    ON r.id_habitacion = h.id_habitacion
                WHERE p.fecha_pago BETWEEN ? AND ?
                ORDER BY p.fecha_pago DESC, p.id_pago DESC
                """;

        return jdbcTemplate.query(
                sql,
                (rs, rowNum) -> {

                    ReportePago pago = new ReportePago();

                    pago.setIdPago(
                            rs.getInt("id_pago")
                    );

                    pago.setIdReserva(
                            rs.getInt("id_reserva")
                    );

                    pago.setNombreCliente(
                            rs.getString("nombre_cliente")
                    );

                    pago.setDescripcionHabitacion(
                            rs.getString("descripcion_habitacion")
                    );

                    pago.setFechaPago(
                            rs.getDate("fecha_pago").toLocalDate()
                    );

                    pago.setMonto(
                            rs.getBigDecimal("monto")
                    );

                    pago.setMetodoPago(
                            rs.getString("metodo_pago")
                    );

                    return pago;
                },
                java.sql.Date.valueOf(desde),
                java.sql.Date.valueOf(hasta)
        );
    }
}