package com.fredy.infinix;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;

/*
 * SALIR: cierra la sesion y vuelve al login.
 * Es como sacar la tarjeta del cajero: el servidor se olvida de quien eras.
 */
@WebServlet("/salir")
public class SalirServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        HttpSession sesion = req.getSession(false);   // false: si no hay, no crear una
        if (sesion != null) {
            sesion.invalidate();                       // borra la sesion y todo lo que tenia
        }
        resp.sendRedirect(req.getContextPath() + "/login.jsp?salio=1");
    }
}
