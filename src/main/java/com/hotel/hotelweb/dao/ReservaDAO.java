package com.hotel.hotelweb.dao;

import com.hotel.hotelweb.entity.Reserva;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class ReservaDAO {

    private final JdbcTemplate jdbcTemplate;

    public ReservaDAO(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    // CONVERTIR UNA FILA EN UN OBJETO RESERVA
    private Reserva mapearReserva(ResultSet rs) throws SQLException {

        Reserva reserva = new Reserva();

        reserva.setIdReserva(rs.getInt("id_reserva"));
        reserva.setIdCliente(rs.getInt("id_cliente"));
        reserva.setIdHabitacion(rs.getInt("id_habitacion"));

        reserva.setFechaInicio(
                rs.getDate("fecha_inicio").toLocalDate()
        );
        reserva.setFechaFin(
                rs.getDate("fecha_fin").toLocalDate()
        );

        reserva.setEstado(rs.getString("estado"));
        reserva.setPrecioNoche(rs.getBigDecimal("precio_noche"));

        Timestamp checkIn = rs.getTimestamp("fecha_check_in");
        Timestamp checkOut = rs.getTimestamp("fecha_check_out");

        reserva.setFechaCheckIn(
                checkIn != null ? checkIn.toLocalDateTime() : null
        );
        reserva.setFechaCheckOut(
                checkOut != null ? checkOut.toLocalDateTime() : null
        );

        return reserva;
    }

    // LISTAR RESERVAS
    public List<Reserva> listarReservas() {

        String sql = """
                SELECT r.id_reserva,
                       r.id_cliente,
                       r.id_habitacion,
                       r.fecha_inicio,
                       r.fecha_fin,
                       r.estado,
                       r.precio_noche,
                       r.fecha_check_in,
                       r.fecha_check_out,
                       CONCAT(c.nombre, ' ', c.apellido)
                           AS nombre_cliente,
                       CONCAT(h.numero, ' - ', h.tipo)
                           AS descripcion_habitacion
                FROM reservas r
                INNER JOIN clientes c
                    ON r.id_cliente = c.id_cliente
                INNER JOIN habitaciones h
                    ON r.id_habitacion = h.id_habitacion
                ORDER BY r.id_reserva DESC
                """;

        return jdbcTemplate.query(sql, (rs, rowNum) -> {

            Reserva reserva = mapearReserva(rs);

            reserva.setNombreCliente(
                    rs.getString("nombre_cliente")
            );
            reserva.setDescripcionHabitacion(
                    rs.getString("descripcion_habitacion")
            );

            return reserva;
        });
    }

    // BUSCAR RESERVA
    public Reserva buscarReserva(int id) {

        return jdbcTemplate.queryForObject(
                """
                SELECT id_reserva, id_cliente, id_habitacion,
                       fecha_inicio, fecha_fin, estado,
                       precio_noche, fecha_check_in, fecha_check_out
                FROM reservas
                WHERE id_reserva = ?
                """,
                (rs, rowNum) -> mapearReserva(rs),
                id
        );
    }

    // VALIDAR DATOS BÁSICOS
    private void validarDatosReserva(Reserva reserva) {

        if (reserva.getIdCliente() <= 0
                || reserva.getIdHabitacion() <= 0) {
            throw new IllegalStateException(
                    "Debes seleccionar un cliente y una habitación."
            );
        }

        if (reserva.getFechaInicio() == null
                || reserva.getFechaFin() == null) {
            throw new IllegalStateException(
                    "Debes completar las fechas de la reserva."
            );
        }

        if (!reserva.getFechaFin().isAfter(
                reserva.getFechaInicio())) {
            throw new IllegalStateException(
                    "La fecha de salida debe ser posterior "
                    + "a la fecha de ingreso."
            );
        }
    }

    // ESTADOS PERMITIDOS EN EL FORMULARIO
    private void validarEstadoFormulario(Reserva reserva) {

        if (!"Reservada".equals(reserva.getEstado())
                && !"Confirmada".equals(reserva.getEstado())) {

            throw new IllegalStateException(
                    "Selecciona Reservada o Confirmada. "
                    + "El ingreso y la salida se registran "
                    + "mediante check-in y check-out."
            );
        }
    }

    // CONSULTAR Y BLOQUEAR LA TARIFA DE LA HABITACIÓN
    private BigDecimal obtenerPrecioHabitacion(int idHabitacion) {

        List<BigDecimal> precios = jdbcTemplate.query(
                """
                SELECT precio
                FROM habitaciones
                WHERE id_habitacion = ?
                FOR UPDATE
                """,
                (rs, rowNum) -> rs.getBigDecimal("precio"),
                idHabitacion
        );

        if (precios.isEmpty()) {
            throw new IllegalStateException(
                    "La habitación seleccionada no existe."
            );
        }

        BigDecimal precio = precios.get(0);

        if (precio == null || precio.signum() < 0) {
            throw new IllegalStateException(
                    "La habitación no tiene una tarifa válida."
            );
        }

        return precio;
    }

    // COMPROBAR FECHAS CRUZADAS
    // La habitación debe estar bloqueada antes de esta consulta.
    private void validarDisponibilidad(
            Reserva reserva,
            int idExcluir) {

        List<Integer> conflictos = jdbcTemplate.query(
                """
                SELECT id_reserva
                FROM reservas
                WHERE id_habitacion = ?
                  AND id_reserva <> ?
                  AND fecha_check_out IS NULL
                  AND (
                      estado IN (
                          'Reservada',
                          'Confirmada',
                          'En alojamiento'
                      )
                      OR fecha_check_in IS NOT NULL
                  )
                  AND fecha_inicio < ?
                  AND fecha_fin > ?
                FOR UPDATE
                """,
                (rs, rowNum) -> rs.getInt("id_reserva"),
                reserva.getIdHabitacion(),
                idExcluir,
                reserva.getFechaFin(),
                reserva.getFechaInicio()
        );

        if (!conflictos.isEmpty()) {
            throw new IllegalStateException(
                    "La habitación ya tiene una reserva "
                    + "que se cruza con las fechas seleccionadas. "
                    + "Selecciona otra habitación o cambia las fechas."
            );
        }
    }

    // PROTEGER LOS DATOS DE RESERVAS CON PAGOS
    private void validarCambiosConPagos(
            Reserva actual,
            Reserva nueva) {

        boolean cambiaDatos =
                actual.getIdCliente() != nueva.getIdCliente()
                || actual.getIdHabitacion() != nueva.getIdHabitacion()
                || !actual.getFechaInicio().equals(
                        nueva.getFechaInicio()
                )
                || !actual.getFechaFin().equals(
                        nueva.getFechaFin()
                );

        if (!cambiaDatos) {
            return;
        }

        List<Integer> pagos = jdbcTemplate.query(
                """
                SELECT id_pago
                FROM pagos
                WHERE id_reserva = ?
                FOR UPDATE
                """,
                (rs, rowNum) -> rs.getInt("id_pago"),
                actual.getIdReserva()
        );

        if (!pagos.isEmpty()) {
            throw new IllegalStateException(
                    "Esta reserva ya tiene pagos registrados. "
                    + "Revisa y corrige sus pagos antes de cambiar "
                    + "el cliente, la habitación o las fechas."
            );
        }
    }

    // GUARDAR NUEVA RESERVA
    @Transactional
    public void guardarReserva(Reserva reserva) {

        validarDatosReserva(reserva);
        validarEstadoFormulario(reserva);

        // Bloquear la habitación antes de verificar las fechas.
        BigDecimal precioNoche = obtenerPrecioHabitacion(
                reserva.getIdHabitacion()
        );

        validarDisponibilidad(reserva, 0);

        jdbcTemplate.update(
                """
                INSERT INTO reservas
                    (id_cliente, id_habitacion, fecha_inicio,
                     fecha_fin, estado, precio_noche)
                VALUES (?, ?, ?, ?, ?, ?)
                """,
                reserva.getIdCliente(),
                reserva.getIdHabitacion(),
                reserva.getFechaInicio(),
                reserva.getFechaFin(),
                reserva.getEstado(),
                precioNoche
        );

        reserva.setPrecioNoche(precioNoche);
    }

    // ACTUALIZAR RESERVA ANTES DEL CHECK-IN
    @Transactional
    public void actualizarReserva(Reserva reserva) {

        Reserva actual = bloquearReserva(reserva.getIdReserva());

        if (actual.getFechaCheckIn() != null
                || actual.getFechaCheckOut() != null) {
            throw new IllegalStateException(
                    "No se puede editar una reserva "
                    + "que ya tiene check-in o check-out."
            );
        }

        validarDatosReserva(reserva);
        validarEstadoFormulario(reserva);
        validarCambiosConPagos(actual, reserva);

        // También se bloquea si mantiene la misma habitación.
        BigDecimal tarifaActual = obtenerPrecioHabitacion(
                reserva.getIdHabitacion()
        );

        validarDisponibilidad(
                reserva,
                reserva.getIdReserva()
        );

        BigDecimal precioNoche = actual.getPrecioNoche();

        if (actual.getIdHabitacion() != reserva.getIdHabitacion()
                || precioNoche == null) {
            precioNoche = tarifaActual;
        }

        jdbcTemplate.update(
                """
                UPDATE reservas
                SET id_cliente = ?, id_habitacion = ?,
                    fecha_inicio = ?, fecha_fin = ?,
                    estado = ?, precio_noche = ?
                WHERE id_reserva = ?
                """,
                reserva.getIdCliente(),
                reserva.getIdHabitacion(),
                reserva.getFechaInicio(),
                reserva.getFechaFin(),
                reserva.getEstado(),
                precioNoche,
                reserva.getIdReserva()
        );

        reserva.setPrecioNoche(precioNoche);
    }

    // ELIMINAR RESERVA ANTES DEL CHECK-IN
    @Transactional
    public void eliminarReserva(int id) {

        Reserva reserva = bloquearReserva(id);

        if (reserva.getFechaCheckIn() != null
                || reserva.getFechaCheckOut() != null) {
            throw new IllegalStateException(
                    "No se puede eliminar una reserva "
                    + "que ya tiene check-in o check-out."
            );
        }

        jdbcTemplate.update(
                "DELETE FROM reservas WHERE id_reserva = ?",
                id
        );
    }

    // BLOQUEAR LA RESERVA DURANTE UNA TRANSACCIÓN
    private Reserva bloquearReserva(int idReserva) {

        List<Reserva> resultados = jdbcTemplate.query(
                """
                SELECT id_reserva, id_cliente, id_habitacion,
                       fecha_inicio, fecha_fin, estado,
                       precio_noche, fecha_check_in, fecha_check_out
                FROM reservas
                WHERE id_reserva = ?
                FOR UPDATE
                """,
                (rs, rowNum) -> mapearReserva(rs),
                idReserva
        );

        if (resultados.isEmpty()) {
            throw new IllegalStateException(
                    "La reserva no existe."
            );
        }

        return resultados.get(0);
    }

    // BLOQUEAR LA HABITACIÓN
    private String bloquearHabitacion(int idHabitacion) {

        return jdbcTemplate.queryForObject(
                """
                SELECT estado
                FROM habitaciones
                WHERE id_habitacion = ?
                FOR UPDATE
                """,
                String.class,
                idHabitacion
        );
    }

    // CONSULTAR OTRAS ESTANCIAS ACTIVAS
    private boolean tieneOtraEstanciaActiva(Reserva reserva) {

        List<Integer> otras = jdbcTemplate.query(
                """
                SELECT id_reserva
                FROM reservas
                WHERE id_habitacion = ?
                  AND id_reserva <> ?
                  AND fecha_check_in IS NOT NULL
                  AND fecha_check_out IS NULL
                FOR UPDATE
                """,
                (rs, rowNum) -> rs.getInt("id_reserva"),
                reserva.getIdHabitacion(),
                reserva.getIdReserva()
        );

        return !otras.isEmpty();
    }

    // REGISTRAR CHECK-IN
    @Transactional
    public void registrarCheckIn(int idReserva) {

        Reserva reserva = bloquearReserva(idReserva);

        if (reserva.getFechaCheckIn() != null) {
            throw new IllegalStateException(
                    "Esta reserva ya tiene un check-in registrado."
            );
        }

        if (reserva.getFechaCheckOut() != null) {
            throw new IllegalStateException(
                    "Esta reserva ya tiene una salida registrada."
            );
        }

        if (!"Reservada".equals(reserva.getEstado())
                && !"Confirmada".equals(reserva.getEstado())) {
            throw new IllegalStateException(
                    "Solo se permite check-in de reservas "
                    + "reservadas o confirmadas."
            );
        }

        validarDatosReserva(reserva);

        LocalDateTime ahora = LocalDateTime.now(
                ZoneId.of("America/Lima")
        );

        LocalDate hoy = ahora.toLocalDate();

        if (hoy.isBefore(reserva.getFechaInicio())
                || !hoy.isBefore(reserva.getFechaFin())) {
            throw new IllegalStateException(
                    "El check-in debe realizarse dentro "
                    + "del periodo reservado y antes "
                    + "de la fecha de salida."
            );
        }

        String estadoHabitacion = bloquearHabitacion(
                reserva.getIdHabitacion()
        );

        if (!"Disponible".equals(estadoHabitacion)) {
            throw new IllegalStateException(
                    "La habitación debe estar Disponible "
                    + "para registrar el check-in."
            );
        }

        if (tieneOtraEstanciaActiva(reserva)) {
            throw new IllegalStateException(
                    "La habitación tiene otra estancia sin check-out."
            );
        }

        validarDisponibilidad(
                reserva,
                reserva.getIdReserva()
        );

        BigDecimal precioNoche = reserva.getPrecioNoche();

        if (precioNoche == null) {
            precioNoche = obtenerPrecioHabitacion(
                    reserva.getIdHabitacion()
            );
        }

        jdbcTemplate.update(
                """
                UPDATE reservas
                SET fecha_check_in = ?,
                    estado = 'En alojamiento',
                    precio_noche = ?
                WHERE id_reserva = ?
                """,
                Timestamp.valueOf(ahora),
                precioNoche,
                idReserva
        );

        jdbcTemplate.update(
                """
                UPDATE habitaciones
                SET estado = 'Ocupada'
                WHERE id_habitacion = ?
                """,
                reserva.getIdHabitacion()
        );
    }

    // COMPROBAR QUE LA CUENTA ESTÉ PAGADA
    private void verificarCuentaPagada(Reserva reserva) {

        validarDatosReserva(reserva);

        if (reserva.getPrecioNoche() == null
                || reserva.getPrecioNoche().signum() < 0) {
            throw new IllegalStateException(
                    "No se puede finalizar la reserva: "
                    + "falta una tarifa válida por noche."
            );
        }

        long noches = ChronoUnit.DAYS.between(
                reserva.getFechaInicio(),
                reserva.getFechaFin()
        );

        BigDecimal totalCuenta =
                reserva.getPrecioNoche().multiply(
                        BigDecimal.valueOf(noches)
                );

        List<BigDecimal> adicionales = jdbcTemplate.query(
                """
                SELECT cantidad * precio_unitario AS subtotal
                FROM consumos_servicios
                WHERE id_reserva = ?
                FOR UPDATE
                """,
                (rs, rowNum) -> rs.getBigDecimal("subtotal"),
                reserva.getIdReserva()
        );

        for (BigDecimal adicional : adicionales) {
            totalCuenta = totalCuenta.add(adicional);
        }

        List<BigDecimal> pagos = jdbcTemplate.query(
                """
                SELECT monto
                FROM pagos
                WHERE id_reserva = ?
                FOR UPDATE
                """,
                (rs, rowNum) -> rs.getBigDecimal("monto"),
                reserva.getIdReserva()
        );

        BigDecimal totalPagado = BigDecimal.ZERO;

        for (BigDecimal pago : pagos) {
            totalPagado = totalPagado.add(pago);
        }

        BigDecimal saldo = totalCuenta.subtract(totalPagado);

        if (saldo.signum() > 0) {
            throw new IllegalStateException(
                    "No se puede registrar el check-out. "
                    + "La reserva tiene un saldo pendiente de S/ "
                    + saldo.toPlainString()
                    + ". Registra el pago antes de finalizar."
            );
        }

        if (saldo.signum() < 0) {
            throw new IllegalStateException(
                    "La reserva tiene pagos superiores a su cuenta. "
                    + "Revisa y corrige los pagos antes "
                    + "de registrar el check-out."
            );
        }
    }

    // REGISTRAR CHECK-OUT
    @Transactional
    public void registrarCheckOut(int idReserva) {

        Reserva reserva = bloquearReserva(idReserva);

        if (reserva.getFechaCheckIn() == null) {
            throw new IllegalStateException(
                    "Primero debes registrar el check-in."
            );
        }

        if (reserva.getFechaCheckOut() != null) {
            throw new IllegalStateException(
                    "Esta reserva ya tiene un check-out registrado."
            );
        }

        if (!"En alojamiento".equals(reserva.getEstado())) {
            throw new IllegalStateException(
                    "Solo se permite check-out de reservas "
                    + "en alojamiento."
            );
        }

        String estadoHabitacion = bloquearHabitacion(
                reserva.getIdHabitacion()
        );

        if (!"Ocupada".equals(estadoHabitacion)) {
            throw new IllegalStateException(
                    "El estado de la habitación no coincide "
                    + "con una estancia activa. Revisa sus datos."
            );
        }

        if (tieneOtraEstanciaActiva(reserva)) {
            throw new IllegalStateException(
                    "La habitación tiene otra estancia activa. "
                    + "Revisa las reservas antes de registrar la salida."
            );
        }

        LocalDateTime ahora = LocalDateTime.now(
                ZoneId.of("America/Lima")
        );

        if (ahora.isBefore(reserva.getFechaCheckIn())) {
            throw new IllegalStateException(
                    "La fecha de salida no puede ser anterior "
                    + "al check-in. Revisa la fecha y hora del equipo."
            );
        }

        verificarCuentaPagada(reserva);

        jdbcTemplate.update(
                """
                UPDATE reservas
                SET fecha_check_out = ?,
                    estado = 'Finalizada'
                WHERE id_reserva = ?
                """,
                Timestamp.valueOf(ahora),
                idReserva
        );

        jdbcTemplate.update(
                """
                UPDATE habitaciones
                SET estado = 'Limpieza'
                WHERE id_habitacion = ?
                """,
                reserva.getIdHabitacion()
        );
    }
}