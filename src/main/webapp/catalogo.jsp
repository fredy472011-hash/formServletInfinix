<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@page import="java.util.List"%>
<%@page import="com.fredy.infinix.Producto"%>
<%
    // Esta pagina la llena CatalogoServlet. Si alguien la abre directo (catalogo.jsp),
    // no hay lista: lo mandamos al servlet, que controla la sesion y trae los productos.
    List<Producto> productos = (List<Producto>) request.getAttribute("productos");
    if (productos == null) {
        response.sendRedirect("catalogo");
        return;
    }
    String error = (String) request.getAttribute("error");

    // El nombre lo guardo LoginServlet en la sesion: sesion.setAttribute("usuario", ...)
    String usuario = (String) session.getAttribute("usuario");
%>
<!DOCTYPE html>
<html lang="es">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Infinix Tech - Catálogo</title>
    <style>
        body { font-family: "Segoe UI", Arial, sans-serif; background: #f1f5f9; margin: 0; }
        header {
            background: #1e3a8a;
            color: white;
            padding: 16px 32px;
            display: flex;
            justify-content: space-between;
            align-items: center;
        }
        header h1 { margin: 0; font-size: 24px; }
        header a { color: white; background: #dc2626; padding: 8px 16px; border-radius: 6px; text-decoration: none; margin-left: 12px; }
        main { max-width: 1100px; margin: 24px auto; padding: 0 16px; }
        .grilla { display: grid; grid-template-columns: repeat(auto-fill, minmax(240px, 1fr)); gap: 16px; }
        .tarjeta {
            background: white;
            border-radius: 10px;
            padding: 18px;
            box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
        }
        .categoria { font-size: 12px; color: #64748b; text-transform: uppercase; }
        .nombre { font-size: 18px; font-weight: bold; color: #0f172a; margin: 6px 0; }
        .marca { color: #1e3a8a; font-weight: bold; }
        .descripcion { color: #475569; font-size: 14px; min-height: 40px; }
        .precio { font-size: 22px; font-weight: bold; color: #16a34a; margin: 10px 0 4px; }
        .stock { font-size: 13px; color: #334155; }
        .agotado { color: #dc2626; font-weight: bold; }
        .error { background: #fee2e2; color: #991b1b; padding: 12px; border-radius: 6px; }
    </style>
</head>
<body>
<header>
    <h1>INFINIX TECH · Catálogo</h1>
    <div>
        Hola, <strong><%= usuario %></strong>
        <%-- href="salir" -> lo atiende SalirServlet, en doGet --%>
        <a href="salir">Salir</a>
    </div>
</header>

<main>
    <% if (error != null) { %>
        <p class="error"><%= error %></p>
    <% } %>

    <div class="grilla">
        <%-- Un "for" que repite la tarjeta por cada producto de la lista --%>
        <% for (Producto p : productos) { %>
            <div class="tarjeta">
                <div class="categoria"><%= p.getCategoria() %> · <%= p.getCodigo() %></div>
                <div class="nombre"><%= p.getNombre() %></div>
                <div class="marca"><%= p.getMarca() %></div>
                <div class="descripcion"><%= p.getDescripcion() %></div>
                <div class="precio">Gs. <%= p.getPrecioConPuntos() %></div>

                <%-- hayStock() viene del diagrama de clases --%>
                <% if (p.hayStock()) { %>
                    <div class="stock">Stock: <%= p.getStock() %> unidades</div>
                <% } else { %>
                    <div class="stock agotado">Sin stock</div>
                <% } %>
            </div>
        <% } %>
    </div>
</main>
</body>
</html>
