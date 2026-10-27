<%--
  La barra azul de arriba, igual en todas las paginas internas.
  Se pega dentro de otra pagina con:   <%@include file="cabecera.jsp"%>
  (es como copiar y pegar este texto ahi, antes de compilar la pagina)
--%>
<%-- Sin esta linea, las tildes salen mal ("CatÃ¡logo"): cada archivo dice en que codificacion esta --%>
<%@page pageEncoding="UTF-8"%>
<%@page import="com.fredy.infinix.CarritoServlet"%>
<%
    // Datos que dejo LoginServlet en la sesion
    String usuarioSesion = (String) session.getAttribute("usuario");
    boolean esAdmin = "ADMIN".equals(session.getAttribute("rol"));
    // Cuantos productos distintos hay en el carrito, para el boton "Carrito (2)"
    int enCarrito = CarritoServlet.obtenerCarrito(session).size();
%>
<header>
    <a href="catalogo" class="marca-tienda">
        <img src="img/logo.svg" alt="" width="54" height="29">
        <span>InfinixTec</span>
    </a>
    <nav>
        <span>Hola, <strong><%= usuarioSesion %></strong></span>
        <a href="catalogo">Catálogo</a>
        <a href="carrito">Carrito (<%= enCarrito %>)</a>
        <%-- Solo el ADMIN ve estos dos --%>
        <% if (esAdmin) { %>
            <a href="admin-productos">Productos</a>
            <a href="reportes.jsp">Reportes</a>
        <% } %>
        <a href="salir" class="salir">Salir</a>
    </nav>
</header>
