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
 *
 * Acepta dos filtros que llegan en la direccion (los dos son opcionales):
 *     catalogo?categoria=Celulares     -> solo celulares
 *     catalogo?buscar=infinix          -> los que tengan "infinix" en el nombre
 *     catalogo?categoria=Celulares&buscar=note   -> los dos juntos
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

        // 2. Los filtros (si no vienen, quedan null)
        String categoria = req.getParameter("categoria");
        String buscar = req.getParameter("buscar");

        // 3. Leer los productos y las categorias (para los botones de arriba)
        List<Producto> productos = new ArrayList<>();   // lista vacia, nunca null
        List<String> categorias = new ArrayList<>();
        String error = null;
        try {
            productos = listarProductos(categoria, buscar);
            categorias = listarCategorias();
        } catch (SQLException e) {
            log("Error al leer los productos", e);
            error = "No se pudieron cargar los productos. Probá más tarde.";
        }

        // 4. Mandar todo a la pagina
        req.setAttribute("productos", productos);
        req.setAttribute("categorias", categorias);
        req.setAttribute("categoriaElegida", categoria);
        req.setAttribute("buscar", buscar);
        req.setAttribute("error", error);
        getServletContext().getRequestDispatcher("/catalogo.jsp").forward(req, resp);
    }

    /*
     * Arma el SELECT segun los filtros que vinieron.
     *
     * Truco del "WHERE 1 = 1": 1 = 1 siempre es verdad, asi que no filtra nada.
     * Sirve para poder agregar cada filtro siempre con "AND ...", sin pensar
     * si es el primero o no.
     *
     *   sin filtros:        SELECT * FROM productos WHERE 1 = 1 ORDER BY ...
     *   con categoria:      ... WHERE 1 = 1 AND categoria = ? ORDER BY ...
     *   con los dos:        ... WHERE 1 = 1 AND categoria = ? AND nombre LIKE ? ORDER BY ...
     *
     * Los valores van SIEMPRE con ? (PreparedStatement), nunca pegados al texto del SQL.
     */
    private List<Producto> listarProductos(String categoria, String buscar) throws SQLException {
        String sql = "SELECT * FROM productos WHERE 1 = 1";
        List<String> valores = new ArrayList<>();   // lo que va en cada ?, en orden

        if (categoria != null && !categoria.isBlank()) {
            sql = sql + " AND categoria = ?";
            valores.add(categoria);
        }
        if (buscar != null && !buscar.isBlank()) {
            // LIKE con % = "contiene". "%note%" encuentra "Infinix Note 40"
            sql = sql + " AND nombre LIKE ?";
            valores.add("%" + buscar.trim() + "%");
        }
        sql = sql + " ORDER BY categoria, nombre";

        List<Producto> lista = new ArrayList<>();
        try (Connection conn = ConexionBd.obtenerConexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            // Llenar los ? en orden: el primero es 1, no 0
            for (int i = 0; i < valores.size(); i++) {
                ps.setString(i + 1, valores.get(i));
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    lista.add(Producto.desdeFila(rs));
                }
            }
        }
        return lista;
    }

    // Las categorias que existen, sin repetir (DISTINCT): Accesorios, Celulares, ...
    private List<String> listarCategorias() throws SQLException {
        String sql = "SELECT DISTINCT categoria FROM productos ORDER BY categoria";
        List<String> lista = new ArrayList<>();
        try (Connection conn = ConexionBd.obtenerConexion();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                lista.add(rs.getString("categoria"));
            }
        }
        return lista;
    }
}
