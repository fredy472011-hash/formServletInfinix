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
import java.util.ArrayList;
import java.util.List;

/*
 * CATALOGO: lee los productos de la base y los manda a catalogo.jsp.
 *
 * Esta PROTEGIDO: si no hay sesion (nadie hizo login), manda al login.
 */
@WebServlet("/catalogo")
public class CatalogoServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        // 1. Hay alguien logueado?
        //    getSession(false) = "dame la sesion, pero si no hay NO la crees"
        HttpSession sesion = req.getSession(false);
        if (sesion == null || sesion.getAttribute("usuario") == null) {
            resp.sendRedirect(req.getContextPath() + "/login.jsp");
            return;   // cortamos aca: no se muestra nada del catalogo
        }

        // 2. Leer los productos
        List<Producto> productos = new ArrayList<>();   // lista vacia, nunca null
        String error = null;
        try {
            productos = listarProductos();
        } catch (SQLException e) {
            log("Error al leer los productos", e);
            error = "No se pudieron cargar los productos. Probá más tarde.";
        }

        // 3. Mandar la lista (y el error, si hubo) a la pagina
        req.setAttribute("productos", productos);
        req.setAttribute("error", error);
        getServletContext().getRequestDispatcher("/catalogo.jsp").forward(req, resp);
    }

    // Igual que tu listar() del Billete: SELECT -> una fila = un objeto -> a la lista
    private List<Producto> listarProductos() throws SQLException {
        String sql = "SELECT codigo, nombre, descripcion, marca, categoria, precio_venta, stock "
                   + "FROM productos ORDER BY categoria, nombre";
        List<Producto> lista = new ArrayList<>();
        try (Connection conn = ConexionBd.obtenerConexion();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                lista.add(new Producto(
                        rs.getString("codigo"),
                        rs.getString("nombre"),
                        rs.getString("descripcion"),
                        rs.getString("marca"),
                        rs.getString("categoria"),
                        rs.getLong("precio_venta"),
                        rs.getInt("stock")));
            }
        }
        return lista;
    }
}
