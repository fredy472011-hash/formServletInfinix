<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@page import="java.util.List"%>
<%
    // Los errores los pone LoginServlet con req.setAttribute("errores", ...) y forward.
    // Si llegamos por primera vez, no hay errores: queda null.
    List<String> errores = (List<String>) request.getAttribute("errores");

    // Avisos que llegan en la direccion (?registrado=1 o ?salio=1) despues de un redirect
    boolean registrado = request.getParameter("registrado") != null;
    boolean salio = request.getParameter("salio") != null;
%>
<!DOCTYPE html>
<html lang="es">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Infinix Tech - Ingresar</title>
    <style>
        body {
            font-family: "Segoe UI", Arial, sans-serif;
            background: linear-gradient(135deg, #0f172a, #1e3a8a);
            min-height: 100vh;
            margin: 0;
            display: flex;
            align-items: center;
            justify-content: center;
        }
        .caja {
            background: white;
            width: 360px;
            padding: 32px 36px;
            border-radius: 12px;
            box-shadow: 0 12px 30px rgba(0, 0, 0, 0.3);
        }
        h1 { margin: 0; color: #1e3a8a; text-align: center; font-size: 28px; }
        .sub { text-align: center; color: #64748b; margin: 4px 0 24px; }
        label { display: block; font-weight: bold; margin-bottom: 4px; color: #334155; }
        input[type="text"], input[type="password"] {
            width: 100%;
            padding: 10px;
            margin-bottom: 14px;
            border: 1px solid #cbd5e1;
            border-radius: 6px;
            box-sizing: border-box;
            font-size: 15px;
        }
        input[type="submit"] {
            width: 100%;
            padding: 12px;
            background: #1e3a8a;
            color: white;
            border: none;
            border-radius: 6px;
            font-size: 16px;
            cursor: pointer;
        }
        input[type="submit"]:hover { background: #1e40af; }
        .errores { background: #fee2e2; color: #991b1b; padding: 10px 14px; border-radius: 6px; list-style: none; margin: 0 0 16px; }
        .exito   { background: #dcfce7; color: #166534; padding: 10px 14px; border-radius: 6px; margin: 0 0 16px; }
        .abajo { text-align: center; margin-top: 18px; color: #475569; }
        .abajo a { color: #1e3a8a; font-weight: bold; }
    </style>
</head>
<body>
<div class="caja">
    <h1>INFINIX TECH</h1>
    <p class="sub">Ingresá para ver el catálogo</p>

    <%-- Avisos verdes --%>
    <% if (registrado) { %>
        <p class="exito">¡Cuenta creada! Ya podés ingresar.</p>
    <% } %>
    <% if (salio) { %>
        <p class="exito">Cerraste la sesión.</p>
    <% } %>

    <%-- Errores rojos (igual que en tu index.jsp de webapp-formFC) --%>
    <% if (errores != null && !errores.isEmpty()) { %>
        <ul class="errores">
            <% for (String error : errores) { %>
                <li><%= error %></li>
            <% } %>
        </ul>
    <% } %>

    <%-- action="login" -> lo atiende LoginServlet (@WebServlet("/login")), en doPost --%>
    <form action="login" method="post">
        <label for="usuario">Usuario</label>
        <input type="text" name="usuario" id="usuario" required>

        <label for="contrasenha">Contraseña</label>
        <input type="password" name="contrasenha" id="contrasenha" required>

        <input type="submit" value="Ingresar">
    </form>

    <p class="abajo">¿No tenés cuenta? <a href="registro.jsp">Registrate</a></p>
</div>
</body>
</html>
