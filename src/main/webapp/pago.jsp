<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@page import="java.util.List"%>
<%@page import="com.fredy.infinix.ItemCarrito"%>
<%@page import="com.fredy.infinix.Producto"%>
<%@page import="com.fredy.infinix.Html"%>
<%
    // La llena PagoServlet. Abierta directo: al servlet.
    List<ItemCarrito> carrito = (List<ItemCarrito>) request.getAttribute("carrito");
    if (carrito == null) {
        response.sendRedirect("pago");
        return;
    }
    long total = (Long) request.getAttribute("total");
    String medio = (String) request.getAttribute("medio");          // debito, credito o qr
    List<String> errores = (List<String>) request.getAttribute("errores");
    boolean conTarjeta = !"qr".equals(medio);
    int[] opcionesCuotas = {1, 3, 6, 12};
%>
<!DOCTYPE html>
<html lang="es">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>InfinixTec - Pago</title>
    <link rel="stylesheet" href="estilos.css">
    <style>
        .pago { display: grid; grid-template-columns: minmax(0, 3fr) minmax(0, 2fr); gap: 24px; align-items: start; }
        @media (max-width: 800px) { .pago { grid-template-columns: 1fr; } }
        .formulario { border-top-left-radius: 0; }
        .prueba { background: #fef9c3; color: #854d0e; padding: 10px 12px; border-radius: 6px; font-size: 14px; }
        .resumen .linea { display: flex; justify-content: space-between; gap: 10px; padding: 6px 0; border-bottom: 1px solid #e2e8f0; font-size: 14px; }
        .qr { text-align: center; }
        .qr img { border: 1px solid #e2e8f0; border-radius: 8px; }
        .pagar { width: 100%; margin-top: 20px; padding: 12px; font-size: 16px; }
    </style>
</head>
<body>
<%@include file="cabecera.jsp"%>

<main>
    <p><a href="carrito">← Volver al carrito</a></p>
    <h2>Elegí cómo pagar</h2>

    <div class="pago">
        <div>
            <%-- Pestanhas: son links comunes (GET pago?medio=...). La pagina se vuelve a armar con la elegida --%>
            <div class="pestanas">
                <a href="pago?medio=debito" class="<%= "debito".equals(medio) ? "elegida" : "" %>">Débito</a>
                <a href="pago?medio=credito" class="<%= "credito".equals(medio) ? "elegida" : "" %>">Crédito</a>
                <a href="pago?medio=qr" class="<%= "qr".equals(medio) ? "elegida" : "" %>">QR</a>
            </div>

            <form class="formulario" action="pago" method="post" autocomplete="off">
                <input type="hidden" name="medio" value="<%= medio %>">

                <% if (!errores.isEmpty()) { %>
                    <ul class="errores">
                        <% for (String error : errores) { %>
                            <li><%= error %></li>
                        <% } %>
                    </ul>
                <% } %>

                <% if (conTarjeta) { %>
                    <p class="prueba">
                        <strong>Modo de prueba:</strong> no uses una tarjeta real.
                        Probá con <strong>4111 1111 1111 1111</strong>, un vencimiento futuro y CVV <strong>123</strong>.
                        El número no se guarda: de la venta solo quedan los últimos 4.
                    </p>

                    <%-- Ninguno de estos campos tiene value="...": si hay un error, se escriben de nuevo.
                         Asi el numero de tarjeta nunca vuelve del servidor a la pagina. --%>
                    <label for="numero">Número de tarjeta</label>
                    <input type="text" id="numero" name="numero" inputmode="numeric" maxlength="19"
                           placeholder="0000 0000 0000 0000" required>

                    <label for="nombre">Nombre como figura en la tarjeta</label>
                    <input type="text" id="nombre" name="nombre" maxlength="60" required>

                    <div class="dos-columnas">
                        <div>
                            <label for="vencimiento">Vencimiento</label>
                            <input type="text" id="vencimiento" name="vencimiento" maxlength="5" placeholder="MM/AA" required>
                        </div>
                        <div>
                            <label for="cvv">CVV</label>
                            <input type="text" id="cvv" name="cvv" inputmode="numeric" maxlength="3" placeholder="123" required>
                        </div>
                    </div>

                    <% if ("credito".equals(medio)) { %>
                        <%-- Diagrama: CondicionVenta.cantidadCuotas --%>
                        <label for="cuotas">Cuotas (sin interés)</label>
                        <select id="cuotas" name="cuotas">
                            <% for (int n : opcionesCuotas) { %>
                                <option value="<%= n %>">
                                    <%= n == 1 ? "1 pago de" : n + " cuotas de" %> Gs. <%= Producto.conPuntos(total / n) %>
                                </option>
                            <% } %>
                        </select>
                    <% } %>

                    <button type="submit" class="boton verde pagar">Pagar Gs. <%= Producto.conPuntos(total) %></button>

                <% } else { %>
                    <div class="qr">
                        <p>Escaneá este código con la app de tu banco <strong>(simulado)</strong>.</p>
                        <%-- El QR lo dibuja QrServlet en el momento, con el total del carrito --%>
                        <img src="qr" alt="Código QR del pago" width="260" height="260">
                        <p class="total">Gs. <%= Producto.conPuntos(total) %></p>
                        <p class="ayuda">Cuando la app te diga que el pago salió, apretá el botón.</p>
                    </div>
                    <button type="submit" class="boton verde pagar">Ya pagué</button>
                <% } %>
            </form>
        </div>

        <%-- Lado derecho: lo que se esta comprando --%>
        <div class="formulario resumen">
            <h3 style="margin-top: 0">Tu pedido</h3>
            <% for (ItemCarrito item : carrito) { %>
                <div class="linea">
                    <span><%= item.getCantidad() %> × <%= Html.escapar(item.getProducto().getNombre()) %></span>
                    <span>Gs. <%= Producto.conPuntos(item.getSubtotal()) %></span>
                </div>
            <% } %>
            <p class="total" style="text-align: right">Total: Gs. <%= Producto.conPuntos(total) %></p>
            <p class="ayuda">Delivery gratis en Carapeguá.</p>
        </div>
    </div>
</main>
<%@include file="pie.jsp"%>
</body>
</html>
