<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@page import="java.util.List"%>
<%@page import="com.fredy.infinix.Producto"%>
<%@page import="com.fredy.infinix.Html"%>
<%
    // La llena ProductoAdminServlet (GET /admin-productos). Abierta directo: al servlet.
    List<Producto> productos = (List<Producto>) request.getAttribute("productos");
    if (productos == null) {
        response.sendRedirect("admin-productos");
        return;
    }
    String avisoOk = (String) request.getAttribute("avisoOk");
    String avisoError = (String) request.getAttribute("avisoError");
%>
<!DOCTYPE html>
<html lang="es">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>InfinixTec - Productos</title>
    <link rel="stylesheet" href="estilos.css">
    <style>
        .arriba { display: flex; justify-content: space-between; align-items: center; margin-bottom: 14px; }
        .chico { padding: 5px 10px; font-size: 13px; }
        td { vertical-align: middle; }
    </style>
</head>
<body>
<%@include file="cabecera.jsp"%>

<main>
    <div class="arriba">
        <h2>Productos (ABM)</h2>
        <a href="admin-productos?accion=nuevo" class="boton verde">+ Nuevo producto</a>
    </div>

    <% if (avisoOk != null) { %>
        <p class="aviso-ok"><%= avisoOk %></p>
    <% } %>
    <% if (avisoError != null) { %>
        <p class="aviso-error"><%= avisoError %></p>
    <% } %>

    <table>
        <tr>
            <th>Foto</th>
            <th>Código</th>
            <th>Nombre</th>
            <th>Categoría</th>
            <th class="derecha">Precio</th>
            <th class="derecha">Stock</th>
            <th>Estado</th>
            <th></th>
        </tr>
        <% for (Producto p : productos) { %>
            <%-- Los dados de baja se ven grises --%>
            <tr class="<%= p.isActivo() ? "" : "de-baja" %>">
                <td><img class="miniatura" src="<%= p.getUrlImagen() %>" alt=""></td>
                <td><%= p.getCodigo() %></td>
                <td><%= Html.escapar(p.getNombre()) %></td>
                <td><%= Html.escapar(p.getCategoria()) %></td>
                <td class="derecha">Gs. <%= p.getPrecioConPuntos() %></td>
                <td class="derecha"><%= p.getStock() %></td>
                <td>
                    <% if (p.isActivo()) { %>
                        <span class="etiqueta activo">Activo</span>
                    <% } else { %>
                        <span class="etiqueta baja">De baja</span>
                    <% } %>
                </td>
                <td class="derecha" style="white-space: nowrap">
                    <%-- Editar: un link (GET) que abre el formulario lleno --%>
                    <a href="admin-productos?accion=editar&codigo=<%= p.getCodigo() %>" class="boton chico">Editar</a>
                    <%-- Baja / Reactivar: cambian la base, por eso van por POST --%>
                    <form class="en-linea" action="admin-productos" method="post">
                        <input type="hidden" name="codigo" value="<%= p.getCodigo() %>">
                        <% if (p.isActivo()) { %>
                            <input type="hidden" name="accion" value="baja">
                            <button class="boton rojo chico">Dar de baja</button>
                        <% } else { %>
                            <input type="hidden" name="accion" value="reactivar">
                            <button class="boton gris chico">Reactivar</button>
                        <% } %>
                    </form>
                </td>
            </tr>
        <% } %>
    </table>
</main>
<%@include file="pie.jsp"%>
</body>
</html>
