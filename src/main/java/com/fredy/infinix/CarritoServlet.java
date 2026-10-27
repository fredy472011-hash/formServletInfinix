package com.fredy.infinix;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/*
 * CARRITO de compras.
 *
 *   GET  /carrito                       -> muestra carrito.jsp
 *   POST /carrito  accion=agregar  codigo=CEL-001   -> suma 1 de ese producto
 *                  accion=sumar    codigo=...       -> boton +
 *                  accion=restar   codigo=...       -> boton -
 *                  accion=quitar   codigo=...       -> boton Quitar
 *                  accion=vaciar                    -> boton Vaciar carrito
 *
 * DONDE SE GUARDA: en la SESION, como una List<ItemCarrito>.
 * No va a la base hasta que el cliente confirma la compra (CompraServlet).
 * Es como el carrito del super: lo vas llenando, y recien en la caja se cobra.
 */
@WebServlet("/carrito")
public class CarritoServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        HttpSession sesion = req.getSession(false);
        if (sesion == null || sesion.getAttribute("usuario") == null) {
            resp.sendRedirect(req.getContextPath() + "/login.jsp");
            return;
        }

        List<ItemCarrito> carrito = obtenerCarrito(sesion);
        req.setAttribute("carrito", carrito);
        req.setAttribute("total", calcularTotal(carrito));

        // Avisos que dejo CompraServlet: se pasan a la pagina y se BORRAN de la sesion,
        // asi se ven una sola vez (si recargas, ya no aparecen).
        req.setAttribute("avisoOk", sesion.getAttribute("avisoOk"));
        req.setAttribute("avisoError", sesion.getAttribute("avisoError"));
        sesion.removeAttribute("avisoOk");
        sesion.removeAttribute("avisoError");

        getServletContext().getRequestDispatcher("/carrito.jsp").forward(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        HttpSession sesion = req.getSession(false);
        if (sesion == null || sesion.getAttribute("usuario") == null) {
            resp.sendRedirect(req.getContextPath() + "/login.jsp");
            return;
        }

        List<ItemCarrito> carrito = obtenerCarrito(sesion);
        String accion = req.getParameter("accion");
        String codigo = req.getParameter("codigo");

        // El item del carrito con ese codigo (null si todavia no esta en el carrito)
        ItemCarrito item = buscarEnCarrito(carrito, codigo);

        if ("agregar".equals(accion)) {
            if (item == null) {
                // Primera vez: lo buscamos en la base para tener sus datos
                try {
                    Producto producto = ProductoServlet.buscarPorCodigo(codigo);
                    if (producto != null && producto.hayStock()) {
                        carrito.add(new ItemCarrito(producto, 1));
                    }
                } catch (SQLException e) {
                    log("Error al agregar al carrito", e);
                }
            } else {
                sumarUno(item);   // ya estaba: uno mas
            }

        } else if ("sumar".equals(accion) && item != null) {
            sumarUno(item);

        } else if ("restar".equals(accion) && item != null) {
            item.setCantidad(item.getCantidad() - 1);
            if (item.getCantidad() == 0) {
                carrito.remove(item);   // si llega a cero, sale del carrito
            }

        } else if ("quitar".equals(accion) && item != null) {
            carrito.remove(item);

        } else if ("vaciar".equals(accion)) {
            carrito.clear();
        }

        /*
         * Despues de un POST, siempre REDIRECT (no forward).
         * Si el usuario aprieta F5 en la pagina del carrito, el navegador repite
         * el GET (mostrar), no el POST (agregar otra vez).
         */
        resp.sendRedirect(req.getContextPath() + "/carrito");
    }

    /*
     * Saca el carrito de la sesion. Si todavia no existe, lo crea vacio y lo guarda.
     * static: tambien lo usan CompraServlet y catalogo.jsp.
     *
     * El (List<ItemCarrito>) de adelante es un CAST: getAttribute devuelve Object,
     * y le decimos a Java que en realidad es una lista de items.
     */
    @SuppressWarnings("unchecked")
    public static List<ItemCarrito> obtenerCarrito(HttpSession sesion) {
        List<ItemCarrito> carrito = (List<ItemCarrito>) sesion.getAttribute("carrito");
        if (carrito == null) {
            carrito = new ArrayList<>();
            sesion.setAttribute("carrito", carrito);
        }
        return carrito;
    }

    // La suma de todos los subtotales. Diagrama de clases: Venta.calcularTotal()
    public static long calcularTotal(List<ItemCarrito> carrito) {
        long total = 0;
        for (ItemCarrito item : carrito) {
            total = total + item.getSubtotal();
        }
        return total;
    }

    // Uno mas, pero nunca mas de los que hay en stock
    private void sumarUno(ItemCarrito item) {
        if (item.getCantidad() < item.getProducto().getStock()) {
            item.setCantidad(item.getCantidad() + 1);
        }
    }

    private ItemCarrito buscarEnCarrito(List<ItemCarrito> carrito, String codigo) {
        for (ItemCarrito item : carrito) {
            if (item.getProducto().getCodigo().equals(codigo)) {
                return item;
            }
        }
        return null;
    }
}
