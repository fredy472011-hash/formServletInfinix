package com.fredy.infinix;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/*
 * LA FOTO de un producto:   imagen?codigo=TEC-001
 *
 * Lo nuevo: este servlet NO devuelve una pagina, devuelve una IMAGEN (bytes).
 * Es la misma idea que el PDF de los reportes: cambia el "Content-Type".
 *     text/html        -> el navegador lo dibuja como pagina
 *     image/png        -> el navegador lo dibuja como imagen
 *     application/pdf  -> el navegador lo abre o lo descarga como PDF
 *
 * Se usa dentro de una pagina asi:   <img src="imagen?codigo=TEC-001">
 * El navegador ve el <img>, pide esa direccion, y este servlet le manda la foto.
 *
 * No pide login: las fotos de los productos no son secretas (como en cualquier tienda).
 */
@WebServlet("/imagen")
public class ImagenServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String codigo = req.getParameter("codigo");
        String sql = "SELECT imagen, imagen_tipo FROM productos WHERE codigo = ? AND imagen IS NOT NULL";

        try (Connection conn = ConexionBd.obtenerConexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, codigo);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    // No existe o no tiene foto: mandamos al dibujo generico
                    resp.sendRedirect(req.getContextPath() + "/img/sin-imagen.svg");
                    return;
                }
                byte[] foto = rs.getBytes("imagen");   // la foto entera, como lista de bytes

                resp.setContentType(rs.getString("imagen_tipo"));   // image/png, image/jpeg...
                resp.setContentLength(foto.length);
                // "no-cache": si en el ABM cambian la foto, el navegador pregunta de nuevo
                resp.setHeader("Cache-Control", "no-cache");
                // Que el navegador crea lo que dice el Content-Type y no "adivine" otro tipo
                resp.setHeader("X-Content-Type-Options", "nosniff");
                resp.getOutputStream().write(foto);
            }
        } catch (SQLException e) {
            log("Error al leer la imagen", e);
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }
}
