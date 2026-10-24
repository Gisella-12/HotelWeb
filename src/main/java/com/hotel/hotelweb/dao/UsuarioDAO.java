package com.hotel.hotelweb.dao;

import java.nio.charset.StandardCharsets;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class UsuarioDAO {

    private final JdbcTemplate jdbcTemplate;

    private final BCryptPasswordEncoder passwordEncoder =
            new BCryptPasswordEncoder(12);

    public UsuarioDAO(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    // VALIDAR EL ACCESO Y ACTUALIZAR LAS CONTRASEÑAS ANTIGUAS
    @Transactional
    public boolean validarUsuario(String usuario, String password) {

        if (usuario == null || password == null
                || usuario.isBlank() || password.isBlank()) {
            return false;
        }

        // BCrypt admite contraseñas de hasta 72 bytes.
        if (password.getBytes(StandardCharsets.UTF_8).length > 72) {
            return false;
        }

        String sql = """
                SELECT id, password_hash
                FROM usuarios
                WHERE usuario = ?
                  AND activo = 1
                FOR UPDATE
                """;

        List<Credencial> resultados = jdbcTemplate.query(
                sql,
                (rs, rowNum) -> new Credencial(
                        rs.getInt("id"),
                        rs.getString("password_hash")
                ),
                usuario.trim()
        );

        if (resultados.size() != 1) {
            return false;
        }

        Credencial credencial = resultados.get(0);
        String hashGuardado = credencial.hash();

        if (hashGuardado == null) {
            return false;
        }

        // VALIDAR CONTRASEÑAS QUE YA UTILIZAN BCRYPT
        if (hashGuardado.startsWith("$2a$")
                || hashGuardado.startsWith("$2b$")
                || hashGuardado.startsWith("$2y$")) {

            return passwordEncoder.matches(
                    password,
                    hashGuardado
            );
        }

        // RECONOCER EL FORMATO ANTIGUO SHA-256
        if (!hashGuardado.matches("[0-9a-fA-F]{64}")) {
            return false;
        }

        String sqlValidarAntigua = """
                SELECT COUNT(*)
                FROM usuarios
                WHERE id = ?
                  AND activo = 1
                  AND BINARY password_hash = BINARY SHA2(?, 256)
                """;

        Integer cantidad = jdbcTemplate.queryForObject(
                sqlValidarAntigua,
                Integer.class,
                credencial.id(),
                password
        );

        if (cantidad == null || cantidad != 1) {
            return false;
        }

        // SOLO TRAS UN ACCESO CORRECTO, MIGRAR A BCRYPT
        String nuevoHash = passwordEncoder.encode(password);

        String sqlActualizar = """
                UPDATE usuarios
                SET password_hash = ?
                WHERE id = ?
                  AND activo = 1
                """;

        int actualizados = jdbcTemplate.update(
                sqlActualizar,
                nuevoHash,
                credencial.id()
        );

        return actualizados == 1;
    }

    // DATOS INTERNOS PARA VALIDAR EL ACCESO
    private record Credencial(int id, String hash) {
    }
}