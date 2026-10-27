package com.fredy.infinix;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.mindrot.jbcrypt.BCrypt;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/*
 * LOGIN: controla usuario y contrasenha. Si estan bien, abre la SESION y manda al catalogo.
 *
 * Recibe el formulario de login.jsp (method="post" action="login").
 */
@WebServlet("/login")
public class LoginServlet extends HttpServlet {

    // Si alguien escribe /login en el navegador (GET), le mostramos el formulario
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.sendRedirect(req.getContextPath() + "/login.jsp");
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String usuario = req.getParameter("usuario");
        String contrasenha = req.getParameter("contrasenha");

        List<String> errores = new ArrayList<>();
        if (usuario == null || usuario.isBlank() || contrasenha == null || contrasenha.isBlank()) {
            errores.add("Escribí el usuario y la contraseña.");
        } else {
            try {
                // Si esta todo bien, loguearse() ya deja en la sesion: usuario, idUsuario y rol
                if (!loguearse(usuario.trim(), contrasenha, req)) {
                    // Mismo mensaje para "no existe" y "contraseña mala":
                    // asi nadie puede averiguar que usuarios existen.
                    errores.add("Usuario o contraseña incorrectos.");
                }
            } catch (SQLException e) {
                log("Error al iniciar sesión", e);
                errores.add("No se pudo conectar con la base de datos. Probá más tarde.");
            }
        }

        if (errores.isEmpty()) {
            // REDIRECT al catalogo (otra direccion)
            resp.sendRedirect(req.getContextPath() + "/catalogo");
        } else {
            // FORWARD: volvemos al login con los errores en la misma pantalla
            req.setAttribute("errores", errores);
            getServletContext().getRequestDispatcher("/login.jsp").forward(req, resp);
        }
    }

    /*
     * Viene del diagrama de clases: Usuario.loguearse(usuario, contrasenha).
     * true si el usuario existe, esta activo y la contrasenha coincide.
     * Si es true, tambien abre la SESION con los datos del usuario.
     */
    private boolean loguearse(String usuario, String contrasenha, HttpServletRequest req)
            throws SQLException {
        String sql = "SELECT id, contrasenha, rol FROM usuarios WHERE usuario = ? AND activo = TRUE";
        try (Connection conn = ConexionBd.obtenerConexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, usuario);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return false;                       // no existe ese usuario
                }
                String hashGuardado = rs.getString("contrasenha");
                // checkpw hace el hash de lo que escribio y lo compara con el guardado
                if (!BCrypt.checkpw(contrasenha, hashGuardado)) {
                    return false;                       // contrasenha mala
                }

                /*
                 * SESION (lo nuevo de este proyecto).
                 *
                 * El servidor no se acuerda de nadie entre un pedido y otro. La sesion es
                 * su "memoria": una cajita por cada navegador, que vive hasta que sale o
                 * pasan 30 minutos sin usarla. Es como la tarjeta metida en el cajero:
                 * mientras esta adentro, el cajero sabe quien sos.
                 *
                 * Guardamos 3 datos:
                 *   usuario   -> para el "Hola, fcanete"
                 *   idUsuario -> para grabar de quien es cada compra (ventas.id_usuario)
                 *   rol       -> CLIENTE o ADMIN (solo ADMIN ve los reportes)
                 */
                HttpSession sesion = req.getSession();   // la crea si no existe
                sesion.setAttribute("usuario", usuario);
                sesion.setAttribute("idUsuario", rs.getInt("id"));
                sesion.setAttribute("rol", rs.getString("rol"));
                return true;
            }
        }
    }
}
