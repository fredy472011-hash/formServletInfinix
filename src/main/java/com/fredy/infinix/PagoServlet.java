package com.fredy.infinix;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

/*
 * PAGO: el paso entre el carrito y la compra.  Diagrama: MedioPago y "Pasarela de pagos".
 *
 *   GET  pago?medio=debito | credito | qr   -> pago.jsp con la pestanha elegida
 *   POST pago  medio=...  (+ datos de la tarjeta)
 *        -> si esta todo bien, "aprueba" el pago y pasa a CompraServlet, que graba la venta
 *        -> si algo esta mal, vuelve a pago.jsp con los errores
 *
 * =====================================================================================
 *  IMPORTANTE: ESTO ES UNA SIMULACION PARA LA FACULTAD.
 *  En una tienda de verdad, los datos de la tarjeta NUNCA pasan por nuestro sistema:
 *  el cliente los escribe en la pagina de la PASARELA (en Paraguay, por ejemplo Bancard),
 *  y la pasarela solo nos contesta "aprobado" o "rechazado". Asi, si a la tienda le
 *  roban la base, no hay tarjetas para robar. Hay una norma mundial que lo exige (PCI DSS).
 *
 *  Por eso aca:
 *    - solo validamos el FORMATO (que parezca una tarjeta),
 *    - NO guardamos el numero ni el CVV en ningun lado (ni base, ni sesion, ni log),
 *    - de la tarjeta solo queda en la venta los ULTIMOS 4 numeros, como en un ticket.
 * =====================================================================================
 */
@WebServlet("/pago")
public class PagoServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        HttpSession sesion = req.getSession(false);
        if (sesion == null || sesion.getAttribute("usuario") == null) {
            resp.sendRedirect(req.getContextPath() + "/login.jsp");
            return;
        }
        if (CarritoServlet.obtenerCarrito(sesion).isEmpty()) {
            resp.sendRedirect(req.getContextPath() + "/carrito");
            return;
        }
        mostrar(req, resp, req.getParameter("medio"), new ArrayList<>());
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        HttpSession sesion = req.getSession(false);
        if (sesion == null || sesion.getAttribute("usuario") == null) {
            resp.sendRedirect(req.getContextPath() + "/login.jsp");
            return;
        }
        if (CarritoServlet.obtenerCarrito(sesion).isEmpty()) {
            resp.sendRedirect(req.getContextPath() + "/carrito");
            return;
        }

        String medio = req.getParameter("medio");
        List<String> errores = new ArrayList<>();
        String ultimosDigitos = null;
        int cuotas = 1;

        if ("debito".equals(medio) || "credito".equals(medio)) {
            // Le sacamos los espacios: "4111 1111 1111 1111" -> "4111111111111111"
            String numero = soloNumeros(req.getParameter("numero"));
            String nombre = req.getParameter("nombre");
            String vencimiento = req.getParameter("vencimiento");
            String cvv = req.getParameter("cvv");

            if (numero.length() != 16) {
                errores.add("El número de tarjeta tiene que tener 16 números.");
            } else if (!pasaLuhn(numero)) {
                errores.add("El número de tarjeta no es válido. Revisá si lo escribiste bien.");
            }
            if (nombre == null || nombre.isBlank()) {
                errores.add("Escribí el nombre como figura en la tarjeta.");
            }
            String errorVencimiento = validarVencimiento(vencimiento);
            if (errorVencimiento != null) {
                errores.add(errorVencimiento);
            }
            if (cvv == null || cvv.length() != 3 || soloNumeros(cvv).length() != 3) {
                errores.add("El código de seguridad (CVV) son los 3 números de atrás de la tarjeta.");
            }
            if ("credito".equals(medio)) {
                // Diagrama: CondicionVenta.cantidadCuotas
                cuotas = leerCuotas(req.getParameter("cuotas"));
                if (cuotas == 0) {
                    errores.add("Elegí la cantidad de cuotas.");
                }
            }
            if (errores.isEmpty()) {
                ultimosDigitos = numero.substring(12);   // los 4 ultimos: posiciones 12 a 15
            }

        } else if (!"qr".equals(medio)) {
            errores.add("Elegí un medio de pago.");
            medio = "debito";
        }

        if (!errores.isEmpty()) {
            mostrar(req, resp, medio, errores);
            return;
        }

        /*
         * Pago "aprobado". Le pasamos a CompraServlet COMO se pago, en atributos del request
         * (no en parametros): asi nadie puede saltarse este paso mandando un POST directo
         * a /comprar, porque los atributos solo los puede poner el servidor.
         * forward: el mismo pedido sigue en CompraServlet (en su doPost).
         */
        req.setAttribute("medioPago", medio.toUpperCase());    // DEBITO, CREDITO o QR
        req.setAttribute("ultimosDigitos", ultimosDigitos);    // null si es QR
        req.setAttribute("cuotas", cuotas);
        getServletContext().getRequestDispatcher("/comprar").forward(req, resp);
    }

    /*
     * Como se pago, en palabras, para el aviso y el reporte:
     *   DEBITO  -> "Débito ****1111"
     *   CREDITO -> "Crédito ****1111 en 6 cuotas"
     *   QR      -> "QR"
     */
    public static String describir(String medio, String ultimosDigitos, int cuotas) {
        if ("DEBITO".equals(medio)) {
            return "Débito ****" + ultimosDigitos;
        }
        if ("CREDITO".equals(medio)) {
            if (cuotas == 1) {
                return "Crédito ****" + ultimosDigitos + " en 1 pago";
            }
            return "Crédito ****" + ultimosDigitos + " en " + cuotas + " cuotas";
        }
        if ("QR".equals(medio)) {
            return "QR";
        }
        return "Efectivo";   // las ventas de antes de que existieran los medios de pago
    }

    // ------------------------------------------------------------------
    // Ayudas
    // ------------------------------------------------------------------

    private void mostrar(HttpServletRequest req, HttpServletResponse resp, String medio, List<String> errores)
            throws ServletException, IOException {
        if (!"credito".equals(medio) && !"qr".equals(medio)) {
            medio = "debito";   // la pestanha que se ve primero
        }
        List<ItemCarrito> carrito = CarritoServlet.obtenerCarrito(req.getSession());
        req.setAttribute("carrito", carrito);
        req.setAttribute("total", CarritoServlet.calcularTotal(carrito));
        req.setAttribute("medio", medio);
        req.setAttribute("errores", errores);
        getServletContext().getRequestDispatcher("/pago.jsp").forward(req, resp);
    }

    // Deja solo los numeros: "4111 1111-1111" -> "411111111111"
    private String soloNumeros(String texto) {
        if (texto == null) {
            return "";
        }
        String resultado = "";
        for (char c : texto.toCharArray()) {
            if (Character.isDigit(c)) {
                resultado = resultado + c;
            }
        }
        return resultado;
    }

    /*
     * ALGORITMO DE LUHN: el ultimo numero de TODA tarjeta es de "control"
     * (como el digito verificador del RUC). Sirve para detectar errores de tipeo.
     *
     *   De derecha a izquierda, se duplica uno si y uno no.
     *   Si al duplicar da mas de 9, se le resta 9.
     *   Se suma todo. Si la suma termina en 0, el numero es valido.
     *
     * 4111 1111 1111 1111 pasa; 4111 1111 1111 1112 no pasa.
     */
    private boolean pasaLuhn(String numero) {
        int suma = 0;
        boolean duplicar = false;
        for (int i = numero.length() - 1; i >= 0; i--) {
            int digito = numero.charAt(i) - '0';   // el caracter '7' -> el numero 7
            if (duplicar) {
                digito = digito * 2;
                if (digito > 9) {
                    digito = digito - 9;
                }
            }
            suma = suma + digito;
            duplicar = !duplicar;
        }
        return suma % 10 == 0;
    }

    /*
     * "MM/AA", por ejemplo "08/28". Devuelve el error, o null si esta bien.
     * YearMonth = un mes de un anho (sin dia). La tarjeta vale hasta el fin de ese mes.
     */
    private String validarVencimiento(String texto) {
        if (texto == null || texto.length() != 5 || texto.charAt(2) != '/') {
            return "El vencimiento va así: MM/AA (por ejemplo 08/28).";
        }
        try {
            int mes = Integer.parseInt(texto.substring(0, 2));
            int anho = 2000 + Integer.parseInt(texto.substring(3, 5));
            if (mes < 1 || mes > 12) {
                return "El mes del vencimiento tiene que ser de 01 a 12.";
            }
            if (YearMonth.of(anho, mes).isBefore(YearMonth.now())) {
                return "La tarjeta está vencida.";
            }
            return null;
        } catch (NumberFormatException e) {
            return "El vencimiento va así: MM/AA (por ejemplo 08/28).";
        }
    }

    // Solo 1, 3, 6 o 12 cuotas. Cualquier otra cosa -> 0 (error)
    private int leerCuotas(String texto) {
        if ("1".equals(texto) || "3".equals(texto) || "6".equals(texto) || "12".equals(texto)) {
            return Integer.parseInt(texto);
        }
        return 0;
    }
}
