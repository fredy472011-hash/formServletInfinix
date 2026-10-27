<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@page import="com.fredy.infinix.Producto"%>
<%
    // Lo llena ProductoServlet. Si alguien abre producto.jsp directo, vuelve al catalogo.
    if (request.getAttribute("producto") == null && request.getAttribute("error") == null) {
        response.sendRedirect("catalogo");
        return;
    }
    Producto p = (Producto) request.getAttribute("producto");
    String error = (String) request.getAttribute("error");
%>
<!DOCTYPE html>
<html lang="es">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Infinix Tech - Producto</title>
    <link rel="stylesheet" href="estilos.css">
    <style>
        .detalle { background: white; border-radius: 10px; padding: 28px; box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08); max-width: 640px; }
        .detalle h2 { margin: 6px 0; font-size: 28px; }
        .detalle dl { display: grid; grid-template-columns: 140px 1fr; gap: 8px; margin: 18px 0; }
        .detalle dt { font-weight: bold; color: #475569; }
    </style>
</head>
<body>
<%@include file="cabecera.jsp"%>

<main>
    <p><a href="catalogo">← Volver al catálogo</a></p>

    <% if (error != null) { %>
        <p class="aviso-error"><%= error %></p>
    <% } else { %>
        <div class="detalle">
            <div class="categoria"><%= p.getCategoria() %></div>
            <h2><%= p.getNombre() %></h2>
            <div class="marca"><%= p.getMarca() %></div>
            <div class="precio">Gs. <%= p.getPrecioConPuntos() %></div>

            <%-- dl = lista de "dato: valor" --%>
            <dl>
                <dt>Código</dt>       <dd><%= p.getCodigo() %></dd>
                <dt>Descripción</dt>  <dd><%= p.getDescripcion() %></dd>
                <dt>Stock</dt>
                <dd>
                    <% if (p.hayStock()) { %>
                        <%= p.getStock() %> unidades
                    <% } else { %>
                        <span class="agotado">Sin stock</span>
                    <% } %>
                </dd>
            </dl>

            <form action="carrito" method="post">
                <input type="hidden" name="accion" value="agregar">
                <input type="hidden" name="codigo" value="<%= p.getCodigo() %>">
                <button type="submit" class="boton verde" <%= p.hayStock() ? "" : "disabled" %>>
                    Agregar al carrito
                </button>
            </form>
        </div>
    <% } %>
</main>
</body>
</html>
