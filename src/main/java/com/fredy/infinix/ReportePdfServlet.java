package com.fredy.infinix;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/*
 * REPORTES EN PDF (solo para ADMIN).  Diagrama de clases: Reporte.exportarPdf()
 *
 *   reporte-pdf?tipo=ventas  -> cada Venta, con su Usuario y sus DetalleVenta (con el Producto)
 *   reporte-pdf?tipo=stock   -> cada Producto, con su stock y si hay que reponer
 *
 * Usa la libreria OpenPDF (esta en el pom.xml). La idea es como armar un Word:
 *   Document  = la hoja en blanco
 *   Paragraph = un parrafo de texto
 *   PdfPTable = una tabla; se llena celda por celda, de izquierda a derecha
 */
@WebServlet("/reporte-pdf")
public class ReportePdfServlet extends HttpServlet {

    // Letras que usamos en los reportes (Helvetica viene incluida en todo PDF)
    private static final Color AZUL = new Color(30, 58, 138);
    private static final Font TITULO = new Font(Font.HELVETICA, 18, Font.BOLD, AZUL);
    private static final Font SUBTITULO = new Font(Font.HELVETICA, 12, Font.BOLD, AZUL);
    private static final Font NORMAL = new Font(Font.HELVETICA, 10, Font.NORMAL);
    private static final Font NEGRITA = new Font(Font.HELVETICA, 10, Font.BOLD);
    private static final Font ENCABEZADO = new Font(Font.HELVETICA, 10, Font.BOLD, Color.WHITE);
    private static final Font ROJO = new Font(Font.HELVETICA, 10, Font.BOLD, new Color(220, 38, 38));

    private static final DateTimeFormatter FECHA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {

        // 1. Solo ADMIN. Si no hay sesion, al login; si es CLIENTE, error 403 ("prohibido")
        HttpSession sesion = req.getSession(false);
        if (sesion == null || sesion.getAttribute("usuario") == null) {
            resp.sendRedirect(req.getContextPath() + "/login.jsp");
            return;
        }
        if (!"ADMIN".equals(sesion.getAttribute("rol"))) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Solo un administrador puede ver los reportes.");
            return;
        }

        String tipo = req.getParameter("tipo");

        /*
         * 2. Armar el PDF en MEMORIA (ByteArrayOutputStream) y recien al final mandarlo.
         *    Asi, si la base falla a la mitad, todavia podemos responder con un error
         *    en vez de mandar un PDF roto.
         */
        ByteArrayOutputStream pdf = new ByteArrayOutputStream();
        try {
            Document documento = new Document(PageSize.A4, 40, 40, 40, 40);   // margenes
            PdfWriter.getInstance(documento, pdf);   // "lo que escribas en documento, ponelo en pdf"
            documento.open();

            if ("stock".equals(tipo)) {
                escribirReporteStock(documento);
            } else {
                escribirReporteVentas(documento);
            }

            documento.close();

        } catch (SQLException | DocumentException e) {
            log("Error al generar el reporte", e);
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "No se pudo generar el reporte.");
            return;
        }

        // 3. Mandar el PDF. "attachment" = el navegador lo descarga con ese nombre
        String archivo = "stock".equals(tipo) ? "reporte-stock.pdf" : "reporte-ventas.pdf";
        resp.setContentType("application/pdf");
        resp.setHeader("Content-Disposition", "attachment; filename=\"" + archivo + "\"");
        resp.setContentLength(pdf.size());
        pdf.writeTo(resp.getOutputStream());
    }

    // ------------------------------------------------------------------
    // Reporte de VENTAS: Venta + Usuario + DetalleVenta + Producto
    // ------------------------------------------------------------------
    private void escribirReporteVentas(Document documento) throws SQLException, DocumentException {
        escribirTitulo(documento, "Reporte de ventas");

        /*
         * JOIN = juntar tablas por su relacion (como las flechas del diagrama de clases):
         *   ventas.id_usuario         -> usuarios.id      (quien compro)
         *   detalle_ventas.id_venta   -> ventas.id        (las lineas de cada venta)
         *   detalle_ventas.id_producto -> productos.id    (que producto es cada linea)
         * Resultado: una fila por cada linea de detalle, con los datos de su venta repetidos.
         */
        String sql = "SELECT v.id, v.fecha, v.total, v.estado, v.medio_pago, v.ultimos_digitos, v.cuotas, u.usuario, "
                   + "       p.codigo, p.nombre, d.cantidad, d.precio_unitario, d.subtotal "
                   + "FROM ventas v "
                   + "JOIN usuarios u       ON u.id = v.id_usuario "
                   + "JOIN detalle_ventas d ON d.id_venta = v.id "
                   + "JOIN productos p      ON p.id = d.id_producto "
                   + "ORDER BY v.id, d.id";

        int ventaActual = -1;        // la venta que estamos escribiendo
        long totalVentaActual = 0;
        PdfPTable tabla = null;
        long totalGeneral = 0;
        int cantidadVentas = 0;

        try (Connection conn = ConexionBd.obtenerConexion();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                int idVenta = rs.getInt("id");

                // Cambio de venta? Cerramos la tabla anterior y empezamos una nueva
                if (idVenta != ventaActual) {
                    if (tabla != null) {
                        cerrarVenta(documento, tabla, totalVentaActual);
                    }
                    ventaActual = idVenta;
                    totalVentaActual = rs.getLong("total");
                    totalGeneral = totalGeneral + totalVentaActual;
                    cantidadVentas++;

                    String fecha = rs.getTimestamp("fecha").toLocalDateTime().format(FECHA_HORA);
                    documento.add(new Paragraph("Venta N.º " + idVenta + "   ·   " + fecha
                            + "   ·   Cliente: " + rs.getString("usuario")
                            + "   ·   Estado: " + rs.getString("estado"), SUBTITULO));
                    // Como se pago (diagrama: MedioPago)
                    documento.add(new Paragraph("Pago: " + PagoServlet.describir(rs.getString("medio_pago"),
                            rs.getString("ultimos_digitos"), rs.getInt("cuotas")), NORMAL));

                    tabla = nuevaTabla(new float[]{2, 5, 1.2f, 2.3f, 2.3f},
                            "Código", "Producto", "Cant.", "Precio unit.", "Subtotal");
                }

                // Una linea de detalle = una fila de la tabla
                celda(tabla, rs.getString("codigo"), NORMAL, Element.ALIGN_LEFT);
                celda(tabla, rs.getString("nombre"), NORMAL, Element.ALIGN_LEFT);
                celda(tabla, String.valueOf(rs.getInt("cantidad")), NORMAL, Element.ALIGN_CENTER);
                celda(tabla, Producto.conPuntos(rs.getLong("precio_unitario")), NORMAL, Element.ALIGN_RIGHT);
                celda(tabla, Producto.conPuntos(rs.getLong("subtotal")), NORMAL, Element.ALIGN_RIGHT);
            }
        }

        if (tabla == null) {
            documento.add(new Paragraph("Todavía no hay ventas registradas.", NORMAL));
            return;
        }
        cerrarVenta(documento, tabla, totalVentaActual);   // la ultima venta

        Paragraph resumen = new Paragraph("Ventas: " + cantidadVentas
                + "      Total vendido: Gs. " + Producto.conPuntos(totalGeneral), TITULO);
        resumen.setAlignment(Element.ALIGN_RIGHT);
        resumen.setSpacingBefore(10);
        documento.add(resumen);
    }

    // Agrega la fila del total y pone la tabla en el documento
    private void cerrarVenta(Document documento, PdfPTable tabla, long total) throws DocumentException {
        PdfPCell etiqueta = new PdfPCell(new Phrase("Total", NEGRITA));
        etiqueta.setColspan(4);                          // ocupa 4 columnas
        etiqueta.setHorizontalAlignment(Element.ALIGN_RIGHT);
        tabla.addCell(etiqueta);
        celda(tabla, "Gs. " + Producto.conPuntos(total), NEGRITA, Element.ALIGN_RIGHT);

        tabla.setSpacingBefore(4);
        tabla.setSpacingAfter(14);
        documento.add(tabla);
    }

    // ------------------------------------------------------------------
    // Reporte de STOCK: cada Producto
    // ------------------------------------------------------------------
    private void escribirReporteStock(Document documento) throws SQLException, DocumentException {
        escribirTitulo(documento, "Reporte de stock");

        PdfPTable tabla = nuevaTabla(new float[]{2, 4.5f, 2.5f, 1.2f, 1.4f, 2},
                "Código", "Producto", "Categoría", "Stock", "Mínimo", "Situación");
        int aReponer = 0;

        // Solo los activos: los dados de baja ya no se reponen
        String sql = "SELECT " + Producto.COLUMNAS + " FROM productos WHERE activo = TRUE ORDER BY categoria, nombre";
        try (Connection conn = ConexionBd.obtenerConexion();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Producto p = Producto.desdeFila(rs);
                celda(tabla, p.getCodigo(), NORMAL, Element.ALIGN_LEFT);
                celda(tabla, p.getNombre(), NORMAL, Element.ALIGN_LEFT);
                celda(tabla, p.getCategoria(), NORMAL, Element.ALIGN_LEFT);
                celda(tabla, String.valueOf(p.getStock()), NORMAL, Element.ALIGN_CENTER);
                celda(tabla, String.valueOf(p.getStockMinimo()), NORMAL, Element.ALIGN_CENTER);
                // necesitaReposicion() viene del diagrama de clases
                if (p.necesitaReposicion()) {
                    celda(tabla, "Reponer", ROJO, Element.ALIGN_CENTER);
                    aReponer++;
                } else {
                    celda(tabla, "OK", NORMAL, Element.ALIGN_CENTER);
                }
            }
        }
        documento.add(tabla);
        documento.add(new Paragraph("Productos a reponer: " + aReponer, SUBTITULO));
    }

    // ------------------------------------------------------------------
    // Ayudas para no repetir codigo
    // ------------------------------------------------------------------

    // El encabezado de todos los reportes
    private void escribirTitulo(Document documento, String titulo) throws DocumentException {
        documento.add(new Paragraph("InfinixTec", TITULO));
        documento.add(new Paragraph("Tu tienda de tecnología · Carapeguá", NORMAL));
        documento.add(new Paragraph(titulo, SUBTITULO));
        Paragraph emitido = new Paragraph("Emitido: " + LocalDateTime.now().format(FECHA_HORA), NORMAL);
        emitido.setSpacingAfter(14);
        documento.add(emitido);
    }

    /*
     * Una tabla nueva con su fila de titulos en azul.
     * "anchos" dice cuanto ocupa cada columna, en proporcion (2 es el doble que 1).
     * "String... titulos" = se le pueden pasar los titulos que quieras, separados por coma.
     */
    private PdfPTable nuevaTabla(float[] anchos, String... titulos) throws DocumentException {
        PdfPTable tabla = new PdfPTable(anchos.length);
        tabla.setWidthPercentage(100);   // todo el ancho de la hoja
        tabla.setWidths(anchos);
        for (String titulo : titulos) {
            PdfPCell celda = new PdfPCell(new Phrase(titulo, ENCABEZADO));
            celda.setBackgroundColor(AZUL);
            celda.setPadding(5);
            tabla.addCell(celda);
        }
        return tabla;
    }

    // Agrega una celda con texto, letra y alineacion
    private void celda(PdfPTable tabla, String texto, Font letra, int alineacion) {
        PdfPCell celda = new PdfPCell(new Phrase(texto, letra));
        celda.setHorizontalAlignment(alineacion);
        celda.setPadding(4);
        tabla.addCell(celda);
    }
}
