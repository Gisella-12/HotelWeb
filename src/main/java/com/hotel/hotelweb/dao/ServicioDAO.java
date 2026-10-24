package com.hotel.hotelweb.dao;

import com.hotel.hotelweb.entity.Servicio;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class ServicioDAO {

    private final JdbcTemplate jdbcTemplate;

    public ServicioDAO(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    // LISTAR SERVICIOS
    public List<Servicio> listarServicios() {

        String sql = """
                SELECT id_servicio,
                       nombre,
                       descripcion,
                       precio
                FROM servicios
                """;

        return jdbcTemplate.query(sql, (rs, rowNum) -> {

            Servicio s = new Servicio();

            s.setIdServicio(
                    rs.getInt("id_servicio")
            );

            s.setNombre(
                    rs.getString("nombre")
            );

            s.setDescripcion(
                    rs.getString("descripcion")
            );

            s.setPrecio(
                    rs.getDouble("precio")
            );

            return s;
        });
    }

    // BUSCAR SERVICIO POR ID
    public Servicio buscarServicio(int id) {

        String sql = """
                SELECT id_servicio,
                       nombre,
                       descripcion,
                       precio
                FROM servicios
                WHERE id_servicio = ?
                """;

        return jdbcTemplate.queryForObject(
                sql,
                (rs, rowNum) -> {

                    Servicio s = new Servicio();

                    s.setIdServicio(
                            rs.getInt("id_servicio")
                    );

                    s.setNombre(
                            rs.getString("nombre")
                    );

                    s.setDescripcion(
                            rs.getString("descripcion")
                    );

                    s.setPrecio(
                            rs.getDouble("precio")
                    );

                    return s;
                },
                id
        );
    }

    // GUARDAR NUEVO SERVICIO
    public void guardarServicio(Servicio servicio) {

        String sql = """
                INSERT INTO servicios
                (nombre, descripcion, precio)
                VALUES (?, ?, ?)
                """;

        jdbcTemplate.update(
                sql,
                servicio.getNombre(),
                servicio.getDescripcion(),
                servicio.getPrecio()
        );
    }

    // ACTUALIZAR SERVICIO
    public void actualizarServicio(Servicio servicio) {

        String sql = """
                UPDATE servicios
                SET nombre = ?,
                    descripcion = ?,
                    precio = ?
                WHERE id_servicio = ?
                """;

        jdbcTemplate.update(
                sql,
                servicio.getNombre(),
                servicio.getDescripcion(),
                servicio.getPrecio(),
                servicio.getIdServicio()
        );
    }

    // ELIMINAR SERVICIO
    public void eliminarServicio(int id) {

        String sql = """
                DELETE FROM servicios
                WHERE id_servicio = ?
                """;

        jdbcTemplate.update(
                sql,
                id
        );
    }
}