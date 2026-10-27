package com.fredy.infinix;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.mindrot.jbcrypt.BCrypt;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/*
 * REGISTRO: crea un usuario nuevo.
 *
 * Recibe el formulario de registro.jsp (method="post" action="registro").
 * Es la misma idea que tu FormServlet: leer parametros -> validar -> guardar.
 *
 *   - Si hay errores: vuelve a registro.jsp con la lista (forward).
 *   - Si salio bien:  manda al login con el aviso "cuenta creada" (redirect).
 */
@WebServlet("/registro")
public class RegistroServlet extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        // 1. Leer lo que escribio el usuario (los "name" de los input)
        String usuario = req.getParameter("usuario");
        String email = req.getParameter("email");
        String contrasenha = req.getParameter("contrasenha");
        String repetir = req.getParameter("repetir");

        // 2. Validar, igual que en tu FormServlet: cada problema se agrega a la lista
        List<String> errores = new ArrayList<>();
        if (usuario == null || usuario.isBlank()) {
            errores.add("El usuario es obligatorio.");
        } else if (!soloLetrasYNumeros(usuario.trim())) {
            errores.add("El usuario solo puede tener letras y números, sin espacios.");
        }
        if (email == null || !email.contains("@")) {
            errores.add("El email es obligatorio y debe tener @.");
        }
        if (contrasenha == null || contrasenha.length() < 6) {
            errores.add("La contraseña debe tener al menos 6 caracteres.");
        } else if (!contrasenha.equals(repetir)) {
            errores.add("Las dos contraseñas no son iguales.");
        }

        // 3. Si los datos estan bien, intentar guardar
        if (errores.isEmpty()) {
            try {
                if (existeUsuario(usuario.trim())) {
                    errores.add("Ese usuario ya existe. Elegí otro.");
                } else {
                    guardarUsuario(usuario.trim(), email.trim(), contrasenha);
                }
            } catch (SQLException e) {
                log("Error al registrar el usuario", e);   // queda escrito en el log de Tomcat
                errores.add("No se pudo conectar con la base de datos. Probá más tarde.");
            }
        }

        // 4. Responder
        if (errores.isEmpty()) {
            // REDIRECT: el navegador va a OTRA direccion (login.jsp).
            // Se usa cuando ya terminamos: si el usuario aprieta F5, no se registra dos veces.
            resp.sendRedirect(req.getContextPath() + "/login.jsp?registrado=1");
        } else {
            // FORWARD: mismo pedido, otra pagina. Asi registro.jsp puede leer los errores.
            req.setAttribute("errores", errores);
            getServletContext().getRequestDispatcher("/registro.jsp").forward(req, resp);
        }
    }

    /*
     * true si el texto tiene solo letras y numeros (ej.: "fredy21").
     * Asi el nombre se puede mostrar tranquilo en "Hola, fredy21": nadie puede
     * meter codigo HTML o JavaScript dentro del usuario.
     */
    private boolean soloLetrasYNumeros(String texto) {
        for (char letra : texto.toCharArray()) {
            if (!Character.isLetterOrDigit(letra)) {
                return false;
            }
        }
        return true;
    }

    // true si ya hay alguien con ese usuario
    private boolean existeUsuario(String usuario) throws SQLException {
        String sql = "SELECT id FROM usuarios WHERE usuario = ?";
        try (Connection conn = ConexionBd.obtenerConexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, usuario);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();   // si hay una fila, existe
            }
        }
    }

    private void guardarUsuario(String usuario, String email, String contrasenha) throws SQLException {
        /*
         * HASH de la contrasenha (lo nuevo de este proyecto).
         *
         * Nunca se guarda la contrasenha real. BCrypt la convierte en un texto como
         *     $2a$10$N9qo8uLOickgx2ZMRZoMye...
         * que NO se puede volver atras. Si alguien roba la base, no ve las contrasenhas.
         *
         * gensalt() agrega una "sal" al azar: dos personas con la misma contrasenha
         * quedan con hashes distintos.
         */
        String hash = BCrypt.hashpw(contrasenha, BCrypt.gensalt());

        String sql = "INSERT INTO usuarios (usuario, email, contrasenha) VALUES (?, ?, ?)";
        try (Connection conn = ConexionBd.obtenerConexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, usuario);
            ps.setString(2, email);
            ps.setString(3, hash);    // se guarda el hash, no la contrasenha
            ps.executeUpdate();
        }
    }
}
