package com.hotel.hotelweb.dao;


import com.hotel.hotelweb.entity.Habitacion;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;


@Repository
public class DashboardDAO {


    private final JdbcTemplate jdbcTemplate;


    public DashboardDAO(JdbcTemplate jdbcTemplate) {

        this.jdbcTemplate = jdbcTemplate;

    }



    // TOTAL HABITACIONES
    public int totalHabitaciones(){

        String sql = """
                SELECT COUNT(*)
                FROM habitaciones
                """;

        return jdbcTemplate.queryForObject(
                sql,
                Integer.class
        );

    }



    // HABITACIONES DISPONIBLES
    public int habitacionesDisponibles(){

        String sql = """
                SELECT COUNT(*)
                FROM habitaciones
                WHERE estado = 'Disponible'
                """;

        return jdbcTemplate.queryForObject(
                sql,
                Integer.class
        );

    }



    // HABITACIONES OCUPADAS
    public int habitacionesOcupadas(){

        String sql = """
                SELECT COUNT(*)
                FROM habitaciones
                WHERE estado = 'Ocupada'
                """;

        return jdbcTemplate.queryForObject(
                sql,
                Integer.class
        );

    }



    // HABITACIONES EN MANTENIMIENTO
    public int habitacionesMantenimiento(){

        String sql = """
                SELECT COUNT(*)
                FROM habitaciones
                WHERE estado = 'Mantenimiento'
                """;

        return jdbcTemplate.queryForObject(
                sql,
                Integer.class
        );

    }



    // TOTAL CLIENTES
    public int totalClientes(){

        String sql = """
                SELECT COUNT(*)
                FROM clientes
                """;

        return jdbcTemplate.queryForObject(
                sql,
                Integer.class
        );

    }



    // TOTAL RESERVAS
    public int totalReservas(){

        String sql = """
                SELECT COUNT(*)
                FROM reservas
                """;

        return jdbcTemplate.queryForObject(
                sql,
                Integer.class
        );

    }



    // TOTAL INGRESOS
    public double totalIngresos(){

        String sql = """
                SELECT COALESCE(SUM(monto),0)
                FROM pagos
                """;

        return jdbcTemplate.queryForObject(
                sql,
                Double.class
        );

    }




    // LISTAR HABITACIONES DEL PANEL
    public List<Habitacion> listarHabitacionesPanel(){


        String sql = """
                SELECT id_habitacion,
                       numero,
                       tipo,
                       precio,
                       estado
                FROM habitaciones
                """;


        return jdbcTemplate.query(sql,(rs,rowNum)->{


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


}