<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@page import="java.util.List"%>
<%
    // Los pone RegistroServlet cuando algo esta mal (forward). La primera vez: null.
    List<String> errores = (List<String>) request.getAttribute("errores");
%>
<!DOCTYPE html>
<html lang="es">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Infinix Tech - Crear cuenta</title>
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
            width: 380px;
            padding: 32px 36px;
            border-radius: 12px;
            box-shadow: 0 12px 30px rgba(0, 0, 0, 0.3);
        }
        h1 { margin: 0; color: #1e3a8a; text-align: center; font-size: 26px; }
        .sub { text-align: center; color: #64748b; margin: 4px 0 24px; }
        label { display: block; font-weight: bold; margin-bottom: 4px; color: #334155; }
        input[type="text"], input[type="email"], input[type="password"] {
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
            background: #16a34a;
            color: white;
            border: none;
            border-radius: 6px;
            font-size: 16px;
            cursor: pointer;
        }
        input[type="submit"]:hover { background: #15803d; }
        .errores { background: #fee2e2; color: #991b1b; padding: 10px 14px 10px 30px; border-radius: 6px; margin: 0 0 16px; }
        .abajo { text-align: center; margin-top: 18px; color: #475569; }
        .abajo a { color: #1e3a8a; font-weight: bold; }
    </style>
</head>
<body>
<div class="caja">
    <h1>Crear cuenta</h1>
    <p class="sub">Infinix Tech</p>

    <% if (errores != null && !errores.isEmpty()) { %>
        <ul class="errores">
            <% for (String error : errores) { %>
                <li><%= error %></li>
            <% } %>
        </ul>
    <% } %>

    <%-- action="registro" -> lo atiende RegistroServlet (@WebServlet("/registro")), en doPost --%>
    <form action="registro" method="post">
        <label for="usuario">Usuario (letras y números)</label>
        <input type="text" name="usuario" id="usuario" required>

        <label for="email">Email</label>
        <input type="email" name="email" id="email" required>

        <label for="contrasenha">Contraseña (mínimo 6)</label>
        <input type="password" name="contrasenha" id="contrasenha" required>

        <label for="repetir">Repetir contraseña</label>
        <input type="password" name="repetir" id="repetir" required>

        <input type="submit" value="Registrarme">
    </form>

    <p class="abajo">¿Ya tenés cuenta? <a href="login.jsp">Ingresá</a></p>
</div>
</body>
</html>
