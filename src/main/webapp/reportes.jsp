<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%
    // Solo ADMIN. Sin sesion -> login. CLIENTE -> catalogo.
    if (session.getAttribute("usuario") == null) {
        response.sendRedirect("login.jsp");
        return;
    }
    if (!"ADMIN".equals(session.getAttribute("rol"))) {
        response.sendRedirect("catalogo");
        return;
    }
%>
<!DOCTYPE html>
<html lang="es">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>InfinixTec - Reportes</title>
    <link rel="stylesheet" href="estilos.css">
    <style>
        .reportes { display: grid; grid-template-columns: repeat(auto-fill, minmax(300px, 1fr)); gap: 16px; }
        .reportes .tarjeta h3 { margin-top: 0; color: #1e3a8a; }
        .reportes .tarjeta p { color: #475569; min-height: 60px; }
    </style>
</head>
<body>
<%@include file="cabecera.jsp"%>

<main>
    <h2>Reportes</h2>
    <p>Se descargan en PDF. Los arma <code>ReportePdfServlet</code> con los datos de la base.</p>

    <div class="reportes">
        <div class="tarjeta">
            <h3>Reporte de ventas</h3>
            <p>Cada venta con el cliente que compró, la fecha, cómo pagó y sus productos
               (cantidad, precio y subtotal). Al final, el total vendido.</p>
            <%-- Link comun: el servlet responde con un PDF en vez de una pagina --%>
            <a href="reporte-pdf?tipo=ventas" class="boton">Descargar PDF</a>
        </div>
        <div class="tarjeta">
            <h3>Reporte de stock</h3>
            <p>Todos los productos con su stock y su stock mínimo.
               Marca en rojo los que hay que reponer.</p>
            <a href="reporte-pdf?tipo=stock" class="boton">Descargar PDF</a>
        </div>
    </div>
</main>
<%@include file="pie.jsp"%>
</body>
</html>
