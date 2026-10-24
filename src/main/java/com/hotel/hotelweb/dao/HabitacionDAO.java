package com.hotel.hotelweb.dao;

import com.hotel.hotelweb.entity.Habitacion;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class HabitacionDAO {

    private final JdbcTemplate jdbcTemplate;

    public HabitacionDAO(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    // LISTAR HABITACIONES
    public List<Habitacion> listarHabitaciones() {

        String sql = """
                SELECT id_habitacion,
                       numero,
                       tipo,
                       precio,
                       estado
                FROM habitaciones
                """;

        return jdbcTemplate.query(sql, (rs, rowNum) -> {

            Habitacion h = new Habitacion();

            h.setIdHabitacion(
                    rs.getInt("id_habitacion")
            );

            h.setNumero(
                    rs.getString("numero")
            );

            h.setTipo(
                    rs.getString("tipo")
            );

            h.setPrecio(
                    rs.getDouble("precio")
            );

            h.setEstado(
                    rs.getString("estado")
            );

            return h;
        });
    }

    // BUSCAR HABITACION POR ID
    public Habitacion buscarPorId(int id) {

        String sql = """
                SELECT id_habitacion,
                       numero,
                       tipo,
                       precio,
                       estado
                FROM habitaciones
                WHERE id_habitacion = ?
                """;

        return jdbcTemplate.queryForObject(
                sql,
                (rs, rowNum) -> {

                    Habitacion h = new Habitacion();

                    h.setIdHabitacion(
                            rs.getInt("id_habitacion")
                    );

                    h.setNumero(
                            rs.getString("numero")
                    );

                    h.setTipo(
                            rs.getString("tipo")
                    );

                    h.setPrecio(
                            rs.getDouble("precio")
                    );

                    h.setEstado(
                            rs.getString("estado")
                    );

                    return h;
                },
                id
        );
    }

    // GUARDAR NUEVA HABITACION
    public void guardarHabitacion(Habitacion habitacion) {

        String sql = """
                INSERT INTO habitaciones
                (numero, tipo, precio, estado)
                VALUES (?, ?, ?, ?)
                """;

        jdbcTemplate.update(
                sql,
                habitacion.getNumero(),
                habitacion.getTipo(),
                habitacion.getPrecio(),
                habitacion.getEstado()
        );
    }

    // ACTUALIZAR HABITACION
    public void actualizarHabitacion(Habitacion habitacion) {

        String sql = """
                UPDATE habitaciones
                SET numero = ?,
                    tipo = ?,
                    precio = ?,
                    estado = ?
                WHERE id_habitacion = ?
                """;

        jdbcTemplate.update(
                sql,
                habitacion.getNumero(),
                habitacion.getTipo(),
                habitacion.getPrecio(),
                habitacion.getEstado(),
                habitacion.getIdHabitacion()
        );
    }

    // ELIMINAR HABITACION
    public void eliminarHabitacion(int id) {

        String sql = """
                DELETE FROM habitaciones
                WHERE id_habitacion = ?
                """;

        jdbcTemplate.update(sql, id);
    }
}