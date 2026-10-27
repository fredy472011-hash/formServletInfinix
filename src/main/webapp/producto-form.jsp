<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@page import="java.util.List"%>
<%@page import="java.util.Map"%>
<%@page import="com.fredy.infinix.Producto"%>
<%@page import="com.fredy.infinix.Html"%>
<%
    // La llena ProductoAdminServlet (nuevo o editar). Abierta directo: a la lista.
    Map<String, String> form = (Map<String, String>) request.getAttribute("form");
    if (form == null) {
        response.sendRedirect("admin-productos");
        return;
    }
    List<String> errores = (List<String>) request.getAttribute("errores");
    List<String> categorias = (List<String>) request.getAttribute("categorias");
    boolean editando = (Boolean) request.getAttribute("editando");
    Producto actual = (Producto) request.getAttribute("producto");   // solo al editar, para la foto
%>
<%!
    // Un METODO dentro del JSP (este bloque empieza con signo de exclamacion).
    // Devuelve lo que va en value="..." de cada campo.
    // getOrDefault: si el campo no esta en el Map, "" (vacio). Html.escapar: por las comillas.
    String valor(Map<String, String> form, String campo) {
        return Html.escapar(form.getOrDefault(campo, ""));
    }
%>
<!DOCTYPE html>
<html lang="es">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>InfinixTec - <%= editando ? "Editar" : "Nuevo" %> producto</title>
    <link rel="stylesheet" href="estilos.css">
    <style>
        .formulario { max-width: 720px; }
        .foto-actual { width: 180px; }
        .botones { margin-top: 20px; display: flex; gap: 10px; }
    </style>
</head>
<body>
<%@include file="cabecera.jsp"%>

<main>
    <p><a href="admin-productos">← Volver a la lista</a></p>
    <h2><%= editando ? "Editar producto " + form.get("codigo") : "Nuevo producto" %></h2>

    <% if (!errores.isEmpty()) { %>
        <ul class="errores">
            <% for (String error : errores) { %>
                <li><%= error %></li>
            <% } %>
        </ul>
    <% } %>

    <%--
      enctype="multipart/form-data": OBLIGATORIO para mandar un archivo.
      Sin eso, del <input type="file"> solo viaja el NOMBRE del archivo, no la foto.
    --%>
    <%--
      accion e id van en la DIRECCION (?accion=guardar&id=5), no como campos ocultos.
      Por que: Tomcat 11 acepta como maximo 10 "partes" en un formulario multipart
      (cada campo es una parte), para protegerse de ataques. Con estos dos afuera, quedan 9.
      El servlet los lee igual con req.getParameter("id").
      id vacio = alta; id con numero = modificacion.
    --%>
    <form class="formulario" action="admin-productos?accion=guardar&id=<%= valor(form, "id") %>"
          method="post" enctype="multipart/form-data">

        <div class="dos-columnas">
            <div>
                <label for="codigo">Código</label>
                <%-- Al editar, el codigo no se puede cambiar (readonly) --%>
                <input type="text" id="codigo" name="codigo" value="<%= valor(form, "codigo") %>"
                       maxlength="20" placeholder="TEC-007" <%= editando ? "readonly" : "required" %>>
            </div>
            <div>
                <label for="categoria">Categoría</label>
                <%-- list="..." + datalist: sugiere las que ya existen, pero deja escribir una nueva --%>
                <input type="text" id="categoria" name="categoria" value="<%= valor(form, "categoria") %>"
                       list="lista-categorias" required>
                <datalist id="lista-categorias">
                    <% for (String c : categorias) { %>
                        <option value="<%= Html.escapar(c) %>">
                    <% } %>
                </datalist>
            </div>
        </div>

        <label for="nombre">Nombre</label>
        <input type="text" id="nombre" name="nombre" value="<%= valor(form, "nombre") %>" maxlength="100" required>

        <label for="descripcion">Descripción</label>
        <input type="text" id="descripcion" name="descripcion" value="<%= valor(form, "descripcion") %>" maxlength="255">

        <div class="dos-columnas">
            <div>
                <label for="marca">Marca</label>
                <input type="text" id="marca" name="marca" value="<%= valor(form, "marca") %>" maxlength="50" required>
            </div>
            <div>
                <label for="precio">Precio (Gs.)</label>
                <input type="text" id="precio" name="precio" value="<%= valor(form, "precio") %>"
                       inputmode="numeric" placeholder="290000" required>
            </div>
            <div>
                <label for="stock">Stock</label>
                <input type="number" id="stock" name="stock" value="<%= valor(form, "stock") %>" min="0" required>
            </div>
            <div>
                <label for="stockMinimo">Stock mínimo</label>
                <input type="number" id="stockMinimo" name="stockMinimo" value="<%= valor(form, "stockMinimo") %>" min="0" required>
            </div>
        </div>

        <label for="imagen">Foto</label>
        <% if (actual != null && actual.tieneImagen()) { %>
            <img class="foto foto-actual" src="<%= actual.getUrlImagen() %>" alt="Foto actual">
            <p class="ayuda">Esta es la foto actual. Elegí otra solo si la querés cambiar.</p>
        <% } %>
        <%-- accept: el explorador de archivos muestra solo imagenes (el servlet igual lo controla) --%>
        <input type="file" id="imagen" name="imagen" accept="image/png, image/jpeg, image/webp">
        <p class="ayuda">JPG, PNG o WEBP, hasta 2 MB.
            <% if (!errores.isEmpty()) { %><strong>Si habías elegido una foto, elegila de nuevo.</strong><% } %>
        </p>

        <div class="botones">
            <button type="submit" class="boton verde"><%= editando ? "Guardar cambios" : "Agregar producto" %></button>
            <a href="admin-productos" class="boton gris">Cancelar</a>
        </div>
    </form>
</main>
<%@include file="pie.jsp"%>
</body>
</html>
