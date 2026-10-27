package com.fredy.infinix;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;

/*
 * EL CODIGO QR del pago:   <img src="qr">
 *
 * Igual que ImagenServlet, no devuelve una pagina sino una imagen PNG.
 * La diferencia: esta imagen no esta guardada en ningun lado, se DIBUJA en el momento.
 *
 * Usa la libreria ZXing (de Google, esta en el pom.xml):
 *   1. QRCodeWriter.encode(texto, ...) -> una BitMatrix: una grilla de cuadritos
 *      negros (true) y blancos (false). Eso es un QR.
 *   2. MatrixToImageWriter -> convierte la grilla en un PNG y lo manda al navegador.
 *
 * El texto lleva el total del carrito. Si lo escaneas con el celular, vas a leer:
 *     InfinixTec | Pago | Gs. 2.090.000
 * (En la vida real, el banco o Bancard te da el contenido del QR; aca es simulado.)
 */
@WebServlet("/qr")
public class QrServlet extends HttpServlet {

    private static final int TAMANHO = 260;   // pixeles de ancho y alto

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        HttpSession sesion = req.getSession(false);
        if (sesion == null || sesion.getAttribute("usuario") == null) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        long total = CarritoServlet.calcularTotal(CarritoServlet.obtenerCarrito(sesion));
        String texto = "InfinixTec | Pago | Gs. " + Producto.conPuntos(total);

        try {
            BitMatrix grilla = new QRCodeWriter().encode(texto, BarcodeFormat.QR_CODE, TAMANHO, TAMANHO);
            resp.setContentType("image/png");
            resp.setHeader("Cache-Control", "no-store");   // el total cambia: no guardarlo
            MatrixToImageWriter.writeToStream(grilla, "PNG", resp.getOutputStream());
        } catch (WriterException e) {
            log("No se pudo dibujar el QR", e);
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }
}
