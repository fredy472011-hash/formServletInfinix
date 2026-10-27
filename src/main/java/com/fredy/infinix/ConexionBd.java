package com.fredy.infinix;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/*
 * La conexion con MySQL. Es la misma de webapp-formFC, cambiando la base.
 *
 * El usuario y la clave NO estan escritos aca: se leen de variables de entorno
 * (DB_USER y DB_PASSWORD). Si no existen, se usa root / root.
 * Asi, en GitHub, nadie ve tu contrasenha.
 */
public class ConexionBd {

    private static final String URL = System.getenv().getOrDefault(
            "DB_URL", "jdbc:mysql://localhost:3306/bd_infinixtech?useSSL=false&serverTimezone=UTC");
    private static final String USER = System.getenv().getOrDefault("DB_USER", "root");
    private static final String PASS = System.getenv().getOrDefault("DB_PASSWORD", "root");

    // private: nadie hace "new ConexionBd()". Se usa directo: ConexionBd.obtenerConexion()
    private ConexionBd() {
    }

    // Siempre dentro de un try (...) para que la conexion se cierre sola
    public static Connection obtenerConexion() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASS);
    }
}
