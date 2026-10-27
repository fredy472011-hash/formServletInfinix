package com.fredy.infinix;

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
import java.sql.Statement;
import java.util.List;

/*
 * CONFIRMAR COMPRA: pasa el carrito (que esta en la sesion) a la base de datos.
 *
 * Graba 3 cosas, y tienen que ir JUNTAS (una TRANSACCION, como en el cajero):
 *   1. una fila en "ventas"            -> la cabecera: quien, cuando, total
 *   2. una fila en "detalle_ventas"    -> por cada producto del carrito
 *   3. baja el stock de cada producto
 *
 * Si algo falla en el medio (por ejemplo, no alcanza el stock de un producto),
 * ROLLBACK: se deshace todo, como si no hubiera pasado nada.
 */
@WebServlet("/comprar")
public class CompraServlet extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {

        HttpSession sesion = req.getSession(false);
        if (sesion == null || sesion.getAttribute("usuario") == null) {
            resp.sendRedirect(req.getContextPath() + "/login.jsp");
            return;
        }

        List<ItemCarrito> carrito = CarritoServlet.obtenerCarrito(sesion);
        if (carrito.isEmpty()) {
            resp.sendRedirect(req.getContextPath() + "/carrito");
            return;
        }

        int idUsuario = (Integer) sesion.getAttribute("idUsuario");

        /*
         * El resultado se avisa con un mensaje que guardamos en la SESION ("avisoOk" o
         * "avisoError"). CarritoServlet lo muestra UNA vez y lo borra.
         * No lo pasamos en la direccion (?mensaje=...) porque cualquiera podria escribir
         * ahi un texto o codigo y hacerlo aparecer en la pagina.
         */
        try {
            int idVenta = registrarVenta(idUsuario, carrito);
            carrito.clear();   // la compra ya esta en la base: el carrito queda vacio
            sesion.setAttribute("avisoOk", "¡Compra confirmada! Venta N.º " + idVenta + ".");

        } catch (SinStockException e) {
            // No alcanzo el stock: el mensaje dice de que producto
            sesion.setAttribute("avisoError", e.getMessage());

        } catch (SQLException e) {
            log("Error al registrar la venta", e);
            sesion.setAttribute("avisoError", "No se pudo registrar la compra. Probá más tarde.");
        }

        resp.sendRedirect(req.getContextPath() + "/carrito");
    }

    /*
     * Devuelve el numero de la venta nueva.
     * Diagrama de clases: Venta.confirmar()
     */
    private int registrarVenta(int idUsuario, List<ItemCarrito> carrito)
            throws SQLException, SinStockException {

        String sqlVenta = "INSERT INTO ventas (id_usuario, total) VALUES (?, ?)";
        String sqlDetalle = "INSERT INTO detalle_ventas "
                + "(id_venta, id_producto, cantidad, precio_unitario, subtotal) VALUES (?, ?, ?, ?, ?)";
        // "AND stock >= ?": solo descuenta si alcanza. Si no alcanza, no cambia ninguna fila.
        String sqlStock = "UPDATE productos SET stock = stock - ? WHERE id = ? AND stock >= ?";

        try (Connection conn = ConexionBd.obtenerConexion()) {
            conn.setAutoCommit(false);   // 1. empieza la transaccion: nada se graba todavia

            try (PreparedStatement psVenta = conn.prepareStatement(sqlVenta, Statement.RETURN_GENERATED_KEYS);
                 PreparedStatement psDetalle = conn.prepareStatement(sqlDetalle);
                 PreparedStatement psStock = conn.prepareStatement(sqlStock)) {

                // La cabecera de la venta
                psVenta.setInt(1, idUsuario);
                psVenta.setLong(2, CarritoServlet.calcularTotal(carrito));
                psVenta.executeUpdate();

                // El id que MySQL le dio a la venta (AUTO_INCREMENT). Igual que en tu FormServlet.
                int idVenta;
                try (ResultSet rs = psVenta.getGeneratedKeys()) {
                    rs.next();
                    idVenta = rs.getInt(1);
                }

                // Por cada producto del carrito: bajar stock + grabar el detalle
                for (ItemCarrito item : carrito) {
                    Producto p = item.getProducto();

                    psStock.setInt(1, item.getCantidad());
                    psStock.setInt(2, p.getId());
                    psStock.setInt(3, item.getCantidad());
                    int filasCambiadas = psStock.executeUpdate();
                    if (filasCambiadas == 0) {
                        // No alcanzo el stock: se corta todo y se deshace
                        conn.rollback();
                        throw new SinStockException("No hay stock suficiente de " + p.getNombre() + ".");
                    }

                    psDetalle.setInt(1, idVenta);
                    psDetalle.setInt(2, p.getId());
                    psDetalle.setInt(3, item.getCantidad());
                    psDetalle.setLong(4, p.getPrecioVenta());
                    psDetalle.setLong(5, item.getSubtotal());
                    psDetalle.executeUpdate();
                }

                conn.commit();           // 2. salio todo bien: se graban las 3 cosas juntas
                return idVenta;

            } catch (SQLException e) {
                conn.rollback();         // 3. algo fallo: se deshace todo
                throw e;                 // y se avisa a doPost
            }
        }
    }

    /*
     * Una excepcion propia, para distinguir "no hay stock" (culpa del pedido)
     * de un error de la base (culpa del sistema). Solo tiene un mensaje.
     */
    private static class SinStockException extends Exception {
        SinStockException(String mensaje) {
            super(mensaje);
        }
    }
}
