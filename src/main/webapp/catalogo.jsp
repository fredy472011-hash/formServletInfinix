<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@page import="java.util.List"%>
<%@page import="java.net.URLEncoder"%>
<%@page import="java.nio.charset.StandardCharsets"%>
<%@page import="com.fredy.infinix.Producto"%>
<%@page import="com.fredy.infinix.Html"%>
<%
    // Esta pagina la llena CatalogoServlet. Si alguien la abre directo (catalogo.jsp),
    // no hay lista: lo mandamos al servlet, que controla la sesion y trae los productos.
    List<Producto> productos = (List<Producto>) request.getAttribute("productos");
    if (productos == null) {
        response.sendRedirect("catalogo");
        return;
    }
    List<String> categorias = (List<String>) request.getAttribute("categorias");
    String categoriaElegida = (String) request.getAttribute("categoriaElegida");
    String buscar = (String) request.getAttribute("buscar");
    String error = (String) request.getAttribute("error");
    boolean hayFiltro = (categoriaElegida != null && !categoriaElegida.isBlank())
                     || (buscar != null && !buscar.isBlank());
%>
<!DOCTYPE html>
<html lang="es">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>InfinixTec - Catálogo</title>
    <link rel="stylesheet" href="estilos.css">
    <style>
        .filtros { display: flex; justify-content: space-between; align-items: center; flex-wrap: wrap; gap: 12px; margin-bottom: 18px; }
        .categorias a { display: inline-block; padding: 7px 14px; margin: 2px; border-radius: 20px; background: white; color: #1e3a8a; text-decoration: none; border: 1px solid #cbd5e1; }
        .categorias a.elegida { background: #1e3a8a; color: white; }
        .buscador input[type="text"] { padding: 8px; border: 1px solid #cbd5e1; border-radius: 6px; width: 200px; }
        .cantidad { color: #64748b; margin-bottom: 12px; }
    </style>
</head>
<body>
<%@include file="cabecera.jsp"%>

<main>
    <% if (error != null) { %>
        <p class="aviso-error"><%= error %></p>
    <% } %>

    <div class="filtros">
        <%-- Botones de categoria: cada uno es un link con ?categoria=... --%>
        <div class="categorias">
            <a href="catalogo" class="<%= hayFiltro ? "" : "elegida" %>">Todas</a>
            <% for (String c : categorias) { %>
                <%-- URLEncoder: si la categoria tuviera espacios o tildes, los pasa a formato de direccion --%>
                <a href="catalogo?categoria=<%= URLEncoder.encode(c, StandardCharsets.UTF_8) %>"
                   class="<%= c.equals(categoriaElegida) ? "elegida" : "" %>"><%= Html.escapar(c) %></a>
            <% } %>
        </div>

        <%-- Buscador: method="get" -> queda en la direccion: catalogo?buscar=note --%>
        <form class="buscador" action="catalogo" method="get">
            <input type="text" name="buscar" placeholder="Buscar por nombre...">
            <input type="submit" class="boton" value="Buscar">
        </form>
    </div>

    <p class="cantidad">
        <%= productos.size() %> producto(s)
        <% if (hayFiltro) { %> · <a href="catalogo">ver todos</a><% } %>
    </p>

    <div class="grilla">
        <%-- Un "for" que repite la tarjeta por cada producto de la lista --%>
        <% for (Producto p : productos) { %>
            <div class="tarjeta">
                <%-- La foto: el navegador pide esa direccion (ImagenServlet) o el dibujo "Sin foto".
                     loading="lazy" = la pide recien cuando la tarjeta aparece en pantalla --%>
                <a href="producto?codigo=<%= p.getCodigo() %>">
                    <img class="foto" src="<%= p.getUrlImagen() %>" alt="<%= Html.escapar(p.getNombre()) %>" loading="lazy">
                </a>
                <div class="categoria"><%= Html.escapar(p.getCategoria()) %> · <%= p.getCodigo() %></div>
                <%-- El nombre es un link al detalle: producto?codigo=CEL-001
                     Html.escapar: el nombre lo cargo alguien en el ABM, lo mostramos "seguro" --%>
                <div class="nombre"><a href="producto?codigo=<%= p.getCodigo() %>"><%= Html.escapar(p.getNombre()) %></a></div>
                <div class="marca"><%= Html.escapar(p.getMarca()) %></div>
                <div class="descripcion"><%= Html.escapar(p.getDescripcion()) %></div>
                <div class="precio">Gs. <%= p.getPrecioConPuntos() %></div>

                <%-- hayStock() viene del diagrama de clases --%>
                <% if (p.hayStock()) { %>
                    <div class="stock">Stock: <%= p.getStock() %> unidades</div>
                <% } else { %>
                    <div class="stock agotado">Sin stock</div>
                <% } %>

                <%-- Agregar al carrito: un formulario chiquito que hace POST a CarritoServlet --%>
                <form action="carrito" method="post">
                    <input type="hidden" name="accion" value="agregar">
                    <input type="hidden" name="codigo" value="<%= p.getCodigo() %>">
                    <button type="submit" class="boton verde" <%= p.hayStock() ? "" : "disabled" %>>
                        Agregar al carrito
                    </button>
                </form>
            </div>
        <% } %>
    </div>
</main>
<%@include file="pie.jsp"%>
</body>
</html>
