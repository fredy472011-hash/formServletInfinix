package com.fredy.infinix;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/*
 * DETALLE de un producto:   producto?codigo=CEL-001
 *
 * Busca ese producto y lo manda a producto.jsp.
 */
@WebServlet("/producto")
public class ProductoServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        // Mismo control que en el catalogo: sin login, al login
        HttpSession sesion = req.getSession(false);
        if (sesion == null || sesion.getAttribute("usuario") == null) {
            resp.sendRedirect(req.getContextPath() + "/login.jsp");
            return;
        }

        String codigo = req.getParameter("codigo");
        Producto producto = null;
        String error = null;
        try {
            producto = buscarPorCodigo(codigo);
            if (producto == null) {
                error = "Ese producto no existe.";
            }
        } catch (SQLException e) {
            log("Error al leer el producto", e);
            error = "No se pudo cargar el producto. Probá más tarde.";
        }

        req.setAttribute("producto", producto);   // puede ser null si no existe
        req.setAttribute("error", error);
        getServletContext().getRequestDispatcher("/producto.jsp").forward(req, resp);
    }

    // El producto con ese codigo, o null si no hay ninguno
    static Producto buscarPorCodigo(String codigo) throws SQLException {
        if (codigo == null || codigo.isBlank()) {
            return null;
        }
        String sql = "SELECT * FROM productos WHERE codigo = ?";
        try (Connection conn = ConexionBd.obtenerConexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, codigo);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Producto.desdeFila(rs);
                }
                return null;
            }
        }
    }
}
