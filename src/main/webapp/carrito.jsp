<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@page import="java.util.List"%>
<%@page import="com.fredy.infinix.ItemCarrito"%>
<%@page import="com.fredy.infinix.Producto"%>
<%
    // Lo llena CarritoServlet (GET /carrito). Si alguien abre carrito.jsp directo, va al servlet.
    List<ItemCarrito> carrito = (List<ItemCarrito>) request.getAttribute("carrito");
    if (carrito == null) {
        response.sendRedirect("carrito");
        return;
    }
    long total = (Long) request.getAttribute("total");
    String avisoOk = (String) request.getAttribute("avisoOk");        // lo deja CompraServlet
    String avisoError = (String) request.getAttribute("avisoError");
%>
<!DOCTYPE html>
<html lang="es">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Infinix Tech - Carrito</title>
    <link rel="stylesheet" href="estilos.css">
    <style>
        .pie { display: flex; justify-content: space-between; align-items: center; margin-top: 18px; flex-wrap: wrap; gap: 12px; }
        .chico { padding: 4px 10px; font-size: 14px; }
    </style>
</head>
<body>
<%@include file="cabecera.jsp"%>

<main>
    <h2>Tu carrito</h2>

    <% if (avisoOk != null) { %>
        <p class="aviso-ok"><%= avisoOk %></p>
    <% } %>
    <% if (avisoError != null) { %>
        <p class="aviso-error"><%= avisoError %></p>
    <% } %>

    <% if (carrito.isEmpty()) { %>
        <p>El carrito está vacío.</p>
        <a href="catalogo" class="boton">Ver el catálogo</a>
    <% } else { %>
        <table>
            <tr>
                <th>Producto</th>
                <th class="derecha">Precio</th>
                <th style="text-align:center">Cantidad</th>
                <th class="derecha">Subtotal</th>
                <th></th>
            </tr>
            <% for (ItemCarrito item : carrito) {
                   Producto p = item.getProducto(); %>
                <tr>
                    <td><strong><%= p.getNombre() %></strong><br><small><%= p.getCodigo() %></small></td>
                    <td class="derecha">Gs. <%= p.getPrecioConPuntos() %></td>
                    <td style="text-align:center">
                        <%-- Cada boton es un formulario chiquito: manda accion + codigo a CarritoServlet --%>
                        <form class="en-linea" action="carrito" method="post">
                            <input type="hidden" name="accion" value="restar">
                            <input type="hidden" name="codigo" value="<%= p.getCodigo() %>">
                            <button class="boton gris chico">−</button>
                        </form>
                        &nbsp;<strong><%= item.getCantidad() %></strong>&nbsp;
                        <form class="en-linea" action="carrito" method="post">
                            <input type="hidden" name="accion" value="sumar">
                            <input type="hidden" name="codigo" value="<%= p.getCodigo() %>">
                            <%-- No deja pasar del stock --%>
                            <button class="boton gris chico" <%= item.getCantidad() < p.getStock() ? "" : "disabled" %>>+</button>
                        </form>
                    </td>
                    <td class="derecha">Gs. <%= Producto.conPuntos(item.getSubtotal()) %></td>
                    <td class="derecha">
                        <form class="en-linea" action="carrito" method="post">
                            <input type="hidden" name="accion" value="quitar">
                            <input type="hidden" name="codigo" value="<%= p.getCodigo() %>">
                            <button class="boton rojo chico">Quitar</button>
                        </form>
                    </td>
                </tr>
            <% } %>
        </table>

        <div class="pie">
            <div>
                <a href="catalogo" class="boton gris">Seguir comprando</a>
                <form class="en-linea" action="carrito" method="post">
                    <input type="hidden" name="accion" value="vaciar">
                    <button class="boton rojo">Vaciar carrito</button>
                </form>
            </div>
            <div>
                <span class="total">Total: Gs. <%= Producto.conPuntos(total) %></span>
                &nbsp;
                <%-- Confirmar: POST a CompraServlet, que graba la venta en la base --%>
                <form class="en-linea" action="comprar" method="post">
                    <button class="boton verde">Confirmar compra</button>
                </form>
            </div>
        </div>
    <% } %>
</main>
</body>
</html>
