package com.hotel.hotelweb.dao;

import com.hotel.hotelweb.entity.Pago;
import com.hotel.hotelweb.entity.Reserva;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class PagoDAO {

    private final JdbcTemplate jdbcTemplate;

    public PagoDAO(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    // CONVERTIR UNA FILA EN UN OBJETO PAGO
    private Pago mapearPago(ResultSet rs) throws SQLException {

        Pago pago = new Pago();

        pago.setIdPago(rs.getInt("id_pago"));
        pago.setIdReserva(rs.getInt("id_reserva"));

        pago.setFechaPago(
                rs.getDate("fecha_pago").toLocalDate()
        );

        pago.setMonto(rs.getDouble("monto"));
        pago.setMetodoPago(rs.getString("metodo_pago"));

        return pago;
    }

    // LISTAR TODOS LOS PAGOS
    public List<Pago> listarPagos() {

        String sql = """
                SELECT id_pago,
                       id_reserva,
                       fecha_pago,
                       monto,
                       metodo_pago
                FROM pagos
                ORDER BY fecha_pago DESC, id_pago DESC
                """;

        return jdbcTemplate.query(
                sql,
                (rs, rowNum) -> mapearPago(rs)
        );
    }

    // BUSCAR PAGO POR ID
    public Pago buscarPago(int id) {

        String sql = """
                SELECT id_pago,
                       id_reserva,
                       fecha_pago,
                       monto,
                       metodo_pago
                FROM pagos
                WHERE id_pago = ?
                """;

        return jdbcTemplate.queryForObject(
                sql,
                (rs, rowNum) -> mapearPago(rs),
                id
        );
    }

    // LISTAR LOS PAGOS DE UNA RESERVA
    public List<Pago> listarPorReserva(int idReserva) {

        String sql = """
                SELECT id_pago,
                       id_reserva,
                       fecha_pago,
                       monto,
                       metodo_pago
                FROM pagos
                WHERE id_reserva = ?
                ORDER BY fecha_pago DESC, id_pago DESC
                """;

        return jdbcTemplate.query(
                sql,
                (rs, rowNum) -> mapearPago(rs),
                idReserva
        );
    }

    // TOTAL PAGADO PARA MOSTRAR EN PANTALLA
    public BigDecimal totalPorReserva(int idReserva) {

        BigDecimal total = jdbcTemplate.queryForObject(
                """
                SELECT COALESCE(SUM(monto), 0)
                FROM pagos
                WHERE id_reserva = ?
                """,
                BigDecimal.class,
                idReserva
        );

        return total != null ? total : BigDecimal.ZERO;
    }

    // VALIDAR LOS DATOS RECIBIDOS
    private BigDecimal validarPago(Pago pago) {

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
                    "El monto debe ser mayor que cero."
            );
        }

        BigDecimal monto = BigDecimal.valueOf(
                pago.getMonto()
        );

        if (monto.stripTrailingZeros().scale() > 2) {
            throw new IllegalStateException(
                    "El monto debe tener como máximo dos decimales."
            );
        }

        if (pago.getMetodoPago() == null
                || pago.getMetodoPago().isBlank()) {

            throw new IllegalStateException(
                    "Debes seleccionar un método de pago."
            );
        }

        pago.setMetodoPago(pago.getMetodoPago().trim());

        return monto;
    }

    // BLOQUEAR LA RESERVA Y VERIFICAR SU ESTADO
    private Reserva bloquearReserva(int idReserva) {

        List<Reserva> reservas = jdbcTemplate.query(
                """
                SELECT id_reserva,
                       fecha_inicio,
                       fecha_fin,
                       precio_noche,
                       estado,
                       fecha_check_out
                FROM reservas
                WHERE id_reserva = ?
                FOR UPDATE
                """,
                (rs, rowNum) -> {

                    Reserva reserva = new Reserva();

                    reserva.setIdReserva(rs.getInt("id_reserva"));

                    if (rs.getDate("fecha_inicio") != null) {
                        reserva.setFechaInicio(
                                rs.getDate("fecha_inicio").toLocalDate()
                        );
                    }

                    if (rs.getDate("fecha_fin") != null) {
                        reserva.setFechaFin(
                                rs.getDate("fecha_fin").toLocalDate()
                        );
                    }

                    reserva.setPrecioNoche(
                            rs.getBigDecimal("precio_noche")
                    );

                    reserva.setEstado(rs.getString("estado"));

                    if (rs.getTimestamp("fecha_check_out") != null) {
                        reserva.setFechaCheckOut(
                                rs.getTimestamp("fecha_check_out")
                                        .toLocalDateTime()
                        );
                    }

                    return reserva;
                },
                idReserva
        );

        if (reservas.isEmpty()) {
            throw new IllegalStateException(
                    "La reserva no existe."
            );
        }

        Reserva reserva = reservas.get(0);

        boolean estadoPermitido =
                "Reservada".equals(reserva.getEstado())
                || "Confirmada".equals(reserva.getEstado())
                || "En alojamiento".equals(reserva.getEstado());

        if (!estadoPermitido
                || reserva.getFechaCheckOut() != null) {

            throw new IllegalStateException(
                    "No se pueden registrar, editar ni eliminar "
                    + "pagos de una reserva finalizada o cancelada."
            );
        }

        return reserva;
    }

    // CALCULAR LA CUENTA CON LOS IMPORTES GUARDADOS
    private BigDecimal calcularTotalCuenta(Reserva reserva) {

        if (reserva.getFechaInicio() == null
                || reserva.getFechaFin() == null
                || !reserva.getFechaFin().isAfter(
                        reserva.getFechaInicio())) {

            throw new IllegalStateException(
                    "La reserva no tiene fechas válidas."
            );
        }

        if (reserva.getPrecioNoche() == null
                || reserva.getPrecioNoche().signum() < 0) {

            throw new IllegalStateException(
                    "La reserva no tiene una tarifa válida guardada. "
                    + "Revisa su precio por noche."
            );
        }

        long noches = ChronoUnit.DAYS.between(
                reserva.getFechaInicio(),
                reserva.getFechaFin()
        );

        BigDecimal alojamiento =
                reserva.getPrecioNoche().multiply(
                        BigDecimal.valueOf(noches)
                );

        List<BigDecimal> subtotales = jdbcTemplate.query(
                """
                SELECT cantidad * precio_unitario AS subtotal
                FROM consumos_servicios
                WHERE id_reserva = ?
                FOR UPDATE
                """,
                (rs, rowNum) -> rs.getBigDecimal("subtotal"),
                reserva.getIdReserva()
        );

        BigDecimal adicionales = BigDecimal.ZERO;

        for (BigDecimal subtotal : subtotales) {
            adicionales = adicionales.add(subtotal);
        }

        return alojamiento.add(adicionales);
    }

    // CONSULTAR LOS OTROS PAGOS DURANTE LA TRANSACCIÓN
    private BigDecimal sumarOtrosPagos(
            int idReserva,
            int idPagoExcluir) {

        List<BigDecimal> montos = jdbcTemplate.query(
                """
                SELECT monto
                FROM pagos
                WHERE id_reserva = ?
                  AND id_pago <> ?
                FOR UPDATE
                """,
                (rs, rowNum) -> rs.getBigDecimal("monto"),
                idReserva,
                idPagoExcluir
        );

        BigDecimal total = BigDecimal.ZERO;

        for (BigDecimal monto : montos) {
            total = total.add(monto);
        }

        return total;
    }

    // VERIFICAR QUE EL PAGO NO SUPERE EL SALDO
    private void validarSaldo(
            Reserva reserva,
            BigDecimal monto,
            int idPagoExcluir) {

        BigDecimal totalCuenta = calcularTotalCuenta(reserva);

        BigDecimal otrosPagos = sumarOtrosPagos(
                reserva.getIdReserva(),
                idPagoExcluir
        );

        BigDecimal disponible = totalCuenta.subtract(otrosPagos);

        if (monto.compareTo(disponible) > 0) {
            throw new IllegalStateException(
                    "El pago supera el saldo disponible de S/ "
                    + disponible.max(BigDecimal.ZERO).toPlainString()
                    + "."
            );
        }
    }

    // CONSULTAR A QUÉ RESERVA PERTENECE EL PAGO
    private int obtenerReservaDelPago(int idPago) {

        List<Integer> reservas = jdbcTemplate.query(
                """
                SELECT id_reserva
                FROM pagos
                WHERE id_pago = ?
                """,
                (rs, rowNum) -> rs.getInt("id_reserva"),
                idPago
        );

        if (reservas.isEmpty()) {
            throw new IllegalStateException(
                    "El pago no existe."
            );
        }

        return reservas.get(0);
    }

    // BLOQUEAR EL PAGO DESPUÉS DE BLOQUEAR SU RESERVA
    private void verificarPagoActual(
            int idPago,
            int idReserva) {

        List<Integer> reservas = jdbcTemplate.query(
                """
                SELECT id_reserva
                FROM pagos
                WHERE id_pago = ?
                FOR UPDATE
                """,
                (rs, rowNum) -> rs.getInt("id_reserva"),
                idPago
        );

        if (reservas.isEmpty()) {
            throw new IllegalStateException(
                    "El pago ya no existe."
            );
        }

        if (reservas.get(0) != idReserva) {
            throw new IllegalStateException(
                    "El pago cambió mientras lo consultabas. "
                    + "Recarga la pantalla."
            );
        }
    }

    // GUARDAR NUEVO PAGO
    @Transactional
    public void guardarPago(Pago pago) {

        BigDecimal monto = validarPago(pago);

        Reserva reserva = bloquearReserva(
                pago.getIdReserva()
        );

        validarSaldo(reserva, monto, 0);

        jdbcTemplate.update(
                """
                INSERT INTO pagos
                    (id_reserva, fecha_pago, monto, metodo_pago)
                VALUES (?, ?, ?, ?)
                """,
                pago.getIdReserva(),
                pago.getFechaPago(),
                monto,
                pago.getMetodoPago()
        );
    }

    // ACTUALIZAR UN PAGO DE LA MISMA RESERVA
    @Transactional
    public void actualizarPago(Pago pago) {

        BigDecimal monto = validarPago(pago);

        int idReserva = obtenerReservaDelPago(
                pago.getIdPago()
        );

        if (idReserva != pago.getIdReserva()) {
            throw new IllegalStateException(
                    "No se puede trasladar un pago a otra reserva."
            );
        }

        Reserva reserva = bloquearReserva(idReserva);

        verificarPagoActual(
                pago.getIdPago(),
                idReserva
        );

        validarSaldo(
                reserva,
                monto,
                pago.getIdPago()
        );

        jdbcTemplate.update(
                """
                UPDATE pagos
                SET fecha_pago = ?,
                    monto = ?,
                    metodo_pago = ?
                WHERE id_pago = ?
                  AND id_reserva = ?
                """,
                pago.getFechaPago(),
                monto,
                pago.getMetodoPago(),
                pago.getIdPago(),
                idReserva
        );
    }

    // ELIMINAR PAGO ANTES DE FINALIZAR LA RESERVA
    @Transactional
    public void eliminarPago(int id) {

        int idReserva = obtenerReservaDelPago(id);

        bloquearReserva(idReserva);

        verificarPagoActual(id, idReserva);

        jdbcTemplate.update(
                """
                DELETE FROM pagos
                WHERE id_pago = ?
                  AND id_reserva = ?
                """,
                id,
                idReserva
        );
    }
}