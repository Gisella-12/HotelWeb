package com.hotel.hotelweb.dao;

import com.hotel.hotelweb.entity.Cliente;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class ClienteDAO {

    private final JdbcTemplate jdbcTemplate;

    public ClienteDAO(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    // LISTAR CLIENTES
    public List<Cliente> listarClientes() {

        String sql = """
                SELECT id_cliente,
                       dni,
                       nombre,
                       apellido,
                       telefono,
                       correo
                FROM clientes
                """;

        return jdbcTemplate.query(sql, (rs, rowNum) -> {

            Cliente c = new Cliente();

            c.setIdCliente(rs.getInt("id_cliente"));
            c.setDni(rs.getString("dni"));
            c.setNombre(rs.getString("nombre"));
            c.setApellido(rs.getString("apellido"));
            c.setTelefono(rs.getString("telefono"));
            c.setCorreo(rs.getString("correo"));

            return c;
        });
    }

    // REGISTRAR CLIENTE
    public void guardarCliente(Cliente cliente) {

        String sql = """
                INSERT INTO clientes
                (dni, nombre, apellido, telefono, correo)
                VALUES (?, ?, ?, ?, ?)
                """;

        jdbcTemplate.update(
                sql,
                cliente.getDni(),
                cliente.getNombre(),
                cliente.getApellido(),
                cliente.getTelefono(),
                cliente.getCorreo()
        );
    }

    // BUSCAR CLIENTE
    public Cliente buscarCliente(int id) {

        String sql = """
                SELECT id_cliente,
                       dni,
                       nombre,
                       apellido,
                       telefono,
                       correo
                FROM clientes
                WHERE id_cliente = ?
                """;

        return jdbcTemplate.queryForObject(
                sql,
                (rs, rowNum) -> {

                    Cliente c = new Cliente();

                    c.setIdCliente(rs.getInt("id_cliente"));
                    c.setDni(rs.getString("dni"));
                    c.setNombre(rs.getString("nombre"));
                    c.setApellido(rs.getString("apellido"));
                    c.setTelefono(rs.getString("telefono"));
                    c.setCorreo(rs.getString("correo"));

                    return c;
                },
                id
        );
    }

    // ACTUALIZAR CLIENTE
    public void actualizarCliente(Cliente cliente) {

        String sql = """
                UPDATE clientes
                SET dni = ?,
                    nombre = ?,
                    apellido = ?,
                    telefono = ?,
                    correo = ?
                WHERE id_cliente = ?
                """;

        jdbcTemplate.update(
                sql,
                cliente.getDni(),
                cliente.getNombre(),
                cliente.getApellido(),
                cliente.getTelefono(),
                cliente.getCorreo(),
                cliente.getIdCliente()
        );
    }

    // ELIMINAR CLIENTE
    public void eliminarCliente(int id) {

        String sql = """
                DELETE FROM clientes
                WHERE id_cliente = ?
                """;

        jdbcTemplate.update(sql, id);
    }
}