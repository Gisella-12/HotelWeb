package com.hotel.hotelweb.dao;

import com.hotel.hotelweb.entity.ConsumoServicio;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class ConsumoServicioDAO {

    private final JdbcTemplate jdbcTemplate;

    public ConsumoServicioDAO(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    // CONSULTAR LOS CONSUMOS DE UNA RESERVA
    public List<ConsumoServicio> listarPorReserva(int idReserva) {

        String sql = """
                SELECT cs.id_consumo,
                       cs.id_reserva,
                       cs.id_servicio,
                       s.nombre AS nombre_servicio,
                       cs.cantidad,
                       cs.precio_unitario,
                       cs.fecha_consumo
                FROM consumos_servicios cs
                INNER JOIN servicios s
                    ON cs.id_servicio = s.id_servicio
                WHERE cs.id_reserva = ?
                ORDER BY cs.fecha_consumo DESC,
                         cs.id_consumo DESC
                """;

        return jdbcTemplate.query(
                sql,
                (rs, rowNum) -> {

                    ConsumoServicio consumo = new ConsumoServicio();

                    consumo.setIdConsumo(
                            rs.getInt("id_consumo")
                    );

                    consumo.setIdReserva(
                            rs.getInt("id_reserva")
                    );

                    consumo.setIdServicio(
                            rs.getInt("id_servicio")
                    );

                    consumo.setNombreServicio(
                            rs.getString("nombre_servicio")
                    );

                    consumo.setCantidad(
                            rs.getInt("cantidad")
                    );

                    consumo.setPrecioUnitario(
                            rs.getBigDecimal("precio_unitario")
                    );

                    consumo.setFechaConsumo(
                            rs.getTimestamp("fecha_consumo")
                                    .toLocalDateTime()
                    );

                    return consumo;
                },
                idReserva
        );
    }

    // CALCULAR EL TOTAL DE ADICIONALES DE UNA RESERVA
    public BigDecimal totalPorReserva(int idReserva) {

        String sql = """
                SELECT COALESCE(
                    SUM(cantidad * precio_unitario), 0
                )
                FROM consumos_servicios
                WHERE id_reserva = ?
                """;

        return jdbcTemplate.queryForObject(
                sql,
                BigDecimal.class,
                idReserva
        );
    }

    // REGISTRAR UN ADICIONAL DURANTE EL ALOJAMIENTO
    @Transactional
    public void registrarConsumo(
            int idReserva,
            int idServicio,
            int cantidad) {

        if (idReserva <= 0 || idServicio <= 0) {
            throw new IllegalStateException(
                    "Selecciona una reserva y un adicional válidos."
            );
        }

        if (cantidad <= 0) {
            throw new IllegalStateException(
                    "La cantidad debe ser mayor que cero."
            );
        }

        // Bloquea la reserva: no permite registrar consumos
        // al mismo tiempo que se finaliza su alojamiento.
        List<Boolean> reservasActivas = jdbcTemplate.query(
                """
                SELECT estado,
                       fecha_check_in,
                       fecha_check_out
                FROM reservas
                WHERE id_reserva = ?
                FOR UPDATE
                """,
                (rs, rowNum) ->
                        "En alojamiento".equals(
                                rs.getString("estado")
                        )
                        && rs.getTimestamp("fecha_check_in") != null
                        && rs.getTimestamp("fecha_check_out") == null,
                idReserva
        );

        if (reservasActivas.isEmpty()) {
            throw new IllegalStateException(
                    "La reserva no existe."
            );
        }

        if (!reservasActivas.get(0)) {
            throw new IllegalStateException(
                    "Solo puedes agregar consumos a una reserva "
                    + "con check-in y sin check-out."
            );
        }

        List<String> nombres = jdbcTemplate.query(
                """
                SELECT nombre
                FROM servicios
                WHERE id_servicio = ?
                FOR UPDATE
                """,
                (rs, rowNum) -> rs.getString("nombre"),
                idServicio
        );

        if (nombres.isEmpty()) {
            throw new IllegalStateException(
                    "El adicional seleccionado no existe."
            );
        }

        String nombre = nombres.get(0).trim();

        boolean esNetflix = "Netflix".equalsIgnoreCase(nombre);
        boolean esGaseosa = "Gaseosa".equalsIgnoreCase(nombre);
        boolean esCerveza = "Cerveza".equalsIgnoreCase(nombre);

        if (!esNetflix && !esGaseosa && !esCerveza) {
            throw new IllegalStateException(
                    "Solo están habilitados Netflix, Gaseosa y Cerveza."
            );
        }

        BigDecimal precio = jdbcTemplate.queryForObject(
                """
                SELECT precio
                FROM servicios
                WHERE id_servicio = ?
                """,
                BigDecimal.class,
                idServicio
        );

        BigDecimal precioEsperado;

        if (esNetflix) {
            precioEsperado = new BigDecimal("10.00");
        } else if (esGaseosa) {
            precioEsperado = new BigDecimal("5.00");
        } else {
            precioEsperado = new BigDecimal("7.00");
        }

        if (precio == null
                || precio.compareTo(precioEsperado) != 0) {

            throw new IllegalStateException(
                    "Revisa el precio del catálogo: Netflix S/ 10, "
                    + "Gaseosa S/ 5 y Cerveza S/ 7."
            );
        }

        if (esNetflix) {

            if (cantidad != 1) {
                throw new IllegalStateException(
                        "Netflix se cobra una sola vez por estancia. "
                        + "La cantidad debe ser 1."
                );
            }

            Integer registrosNetflix = jdbcTemplate.queryForObject(
                    """
                    SELECT COUNT(*)
                    FROM consumos_servicios cs
                    INNER JOIN servicios s
                        ON cs.id_servicio = s.id_servicio
                    WHERE cs.id_reserva = ?
                      AND LOWER(TRIM(s.nombre)) = 'netflix'
                    """,
                    Integer.class,
                    idReserva
            );

            if (registrosNetflix != null && registrosNetflix > 0) {
                throw new IllegalStateException(
                        "Esta reserva ya tiene Netflix registrado."
                );
            }
        }

        LocalDateTime ahora = LocalDateTime.now(
                ZoneId.of("America/Lima")
        );

        // El precio se obtiene de MySQL, no del formulario.
        // Se conserva como precio histórico del consumo.
        jdbcTemplate.update(
                """
                INSERT INTO consumos_servicios
                    (id_reserva,
                     id_servicio,
                     cantidad,
                     precio_unitario,
                     fecha_consumo)
                VALUES (?, ?, ?, ?, ?)
                """,
                idReserva,
                idServicio,
                cantidad,
                precio,
                Timestamp.valueOf(ahora)
        );
    }
}