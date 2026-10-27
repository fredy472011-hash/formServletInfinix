package com.fredy.infinix;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.Part;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/*
 * ABM DE PRODUCTOS (Alta, Baja, Modificacion). Solo ADMIN.
 *
 *   GET  admin-productos                         -> la lista (admin-productos.jsp)
 *   GET  admin-productos?accion=nuevo            -> formulario vacio (producto-form.jsp)
 *   GET  admin-productos?accion=editar&codigo=X  -> formulario con los datos de X
 *   POST admin-productos?accion=guardar&id=...   -> alta (id vacio) o modificacion, con la foto
 *   POST accion=baja      codigo=X               -> activo = FALSE
 *   POST accion=reactivar codigo=X               -> activo = TRUE
 *
 * LO NUEVO: SUBIR UN ARCHIVO (la foto).
 *   - En el formulario:  <form method="post" enctype="multipart/form-data">
 *                        <input type="file" name="imagen">
 *     "multipart" = el formulario viaja en partes: una por cada campo, y la foto en la suya.
 *   - En el servlet: @MultipartConfig (abajo) le avisa a Tomcat que lea esas partes.
 *     Los campos de texto se siguen leyendo con req.getParameter("nombre").
 *     La foto se lee con req.getPart("imagen").
 *
 * BAJA LOGICA: "dar de baja" NO borra la fila (DELETE). Pone activo = FALSE.
 * Las ventas viejas apuntan a ese producto (detalle_ventas.id_producto); si lo
 * borraramos, el reporte de ventas se romperia. Igual que tu Billete con estaActivo.
 */
@WebServlet("/admin-productos")
@MultipartConfig(maxFileSize = 10 * 1024 * 1024, maxRequestSize = 11 * 1024 * 1024)
public class ProductoAdminServlet extends HttpServlet {

    // Aceptamos hasta 10 MB para poder avisar bien; pero guardamos solo hasta 2 MB
    private static final long MAXIMO_IMAGEN = 2 * 1024 * 1024;

    // Los campos del formulario, en el orden en que aparecen
    private static final String[] CAMPOS =
            {"id", "codigo", "nombre", "descripcion", "marca", "categoria", "precio", "stock", "stockMinimo"};

    // ------------------------------------------------------------------
    // GET: mostrar la lista o el formulario
    // ------------------------------------------------------------------
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        if (!esAdmin(req, resp)) {
            return;
        }
        String accion = req.getParameter("accion");

        try {
            if ("nuevo".equals(accion)) {
                mostrarFormulario(req, resp, new HashMap<>(), new ArrayList<>(), false);
                return;
            }

            if ("editar".equals(accion)) {
                Producto p = ProductoServlet.buscarPorCodigo(req.getParameter("codigo"));
                if (p == null) {
                    resp.sendRedirect(req.getContextPath() + "/admin-productos");
                    return;
                }
                // Llenamos el formulario con lo que hay en la base
                Map<String, String> form = new HashMap<>();
                form.put("id", String.valueOf(p.getId()));
                form.put("codigo", p.getCodigo());
                form.put("nombre", p.getNombre());
                form.put("descripcion", p.getDescripcion());
                form.put("marca", p.getMarca());
                form.put("categoria", p.getCategoria());
                form.put("precio", String.valueOf(p.getPrecioVenta()));
                form.put("stock", String.valueOf(p.getStock()));
                form.put("stockMinimo", String.valueOf(p.getStockMinimo()));
                req.setAttribute("producto", p);   // para mostrar la foto actual
                mostrarFormulario(req, resp, form, new ArrayList<>(), true);
                return;
            }

            // Sin accion: la lista
            req.setAttribute("productos", listarTodos());

        } catch (SQLException e) {
            log("Error en el ABM de productos", e);
            req.setAttribute("productos", new ArrayList<Producto>());
            req.setAttribute("avisoError", "No se pudieron leer los productos. Probá más tarde.");
        }

        // Avisos de la ultima accion (los deja doPost en la sesion), se muestran una vez
        HttpSession sesion = req.getSession();
        if (req.getAttribute("avisoError") == null) {
            req.setAttribute("avisoError", sesion.getAttribute("avisoError"));
        }
        req.setAttribute("avisoOk", sesion.getAttribute("avisoOk"));
        sesion.removeAttribute("avisoOk");
        sesion.removeAttribute("avisoError");

        getServletContext().getRequestDispatcher("/admin-productos.jsp").forward(req, resp);
    }

    // ------------------------------------------------------------------
    // POST: guardar, dar de baja o reactivar
    // ------------------------------------------------------------------
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        if (!esAdmin(req, resp)) {
            return;
        }
        HttpSession sesion = req.getSession();
        String accion = req.getParameter("accion");
        String codigo = req.getParameter("codigo");

        try {
            if ("baja".equals(accion) || "reactivar".equals(accion)) {
                boolean activo = "reactivar".equals(accion);
                cambiarActivo(codigo, activo);
                sesion.setAttribute("avisoOk", activo
                        ? "Producto " + codigo + " reactivado: vuelve a verse en el catálogo."
                        : "Producto " + codigo + " dado de baja: ya no se ve en el catálogo.");
                resp.sendRedirect(req.getContextPath() + "/admin-productos");
                return;
            }

            guardar(req, resp);

        } catch (SQLException e) {
            log("Error al guardar el producto", e);
            sesion.setAttribute("avisoError", "No se pudo guardar el producto. Probá más tarde.");
            resp.sendRedirect(req.getContextPath() + "/admin-productos");
        }
    }

    /*
     * Alta (si no viene "id") o modificacion (si viene "id").
     * Paso 1: leer los campos.  Paso 2: validar.  Paso 3: leer la foto.  Paso 4: grabar.
     */
    private void guardar(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException, SQLException {

        // 1. Los campos de texto, en un Map (nombre del campo -> lo que escribio)
        Map<String, String> form = new HashMap<>();
        for (String campo : CAMPOS) {
            String valor = req.getParameter(campo);
            form.put(campo, valor == null ? "" : valor.trim());
        }
        boolean editando = !form.get("id").isEmpty();
        List<String> errores = new ArrayList<>();

        // 2. Validar (como en RegistroServlet: juntamos todos los errores)
        String codigo = form.get("codigo").toUpperCase();
        form.put("codigo", codigo);
        if (!editando) {   // el codigo solo se elige en el alta; despues no cambia
            if (codigo.isEmpty() || codigo.length() > 20 || !esCodigoValido(codigo)) {
                errores.add("El código es obligatorio: letras, números y guiones (por ejemplo TEC-007).");
            } else if (ProductoServlet.buscarPorCodigo(codigo) != null) {
                errores.add("Ya existe un producto con el código " + codigo + ".");
            }
        }
        if (form.get("nombre").isEmpty()) {
            errores.add("El nombre es obligatorio.");
        }
        if (form.get("marca").isEmpty()) {
            errores.add("La marca es obligatoria.");
        }
        if (form.get("categoria").isEmpty()) {
            errores.add("La categoría es obligatoria.");
        }
        long precio = leerNumero(form.get("precio"), "El precio", errores);
        long stock = leerNumero(form.get("stock"), "El stock", errores);
        long stockMinimo = leerNumero(form.get("stockMinimo"), "El stock mínimo", errores);

        // 3. La foto (es opcional: si no eligen ninguna, queda la que tenia o ninguna)
        byte[] imagen = null;
        String tipo = null;
        Part parte;
        try {
            parte = req.getPart("imagen");
        } catch (IllegalStateException e) {
            // Tomcat la rechazo por pasar los 10 MB de @MultipartConfig
            parte = null;
            errores.add("La imagen pesa más de 2 MB. Elegí una más liviana.");
        }
        if (parte != null && parte.getSize() > 0) {
            if (parte.getSize() > MAXIMO_IMAGEN) {
                errores.add("La imagen pesa más de 2 MB. Elegí una más liviana.");
            } else {
                try (InputStream entrada = parte.getInputStream()) {
                    imagen = entrada.readAllBytes();   // la foto entera, en bytes
                }
                tipo = tipoDeImagen(imagen);
                if (tipo == null) {
                    errores.add("La imagen tiene que ser JPG, PNG o WEBP.");
                    imagen = null;
                }
            }
        }

        if (!errores.isEmpty()) {
            if (editando) {
                req.setAttribute("producto", ProductoServlet.buscarPorCodigo(codigo));
            }
            mostrarFormulario(req, resp, form, errores, editando);
            return;
        }

        // 4. Grabar
        if (editando) {
            modificar(Integer.parseInt(form.get("id")), form, precio, (int) stock, (int) stockMinimo, imagen, tipo);
            req.getSession().setAttribute("avisoOk", "Producto " + codigo + " modificado.");
        } else {
            insertar(codigo, form, precio, (int) stock, (int) stockMinimo, imagen, tipo);
            req.getSession().setAttribute("avisoOk", "Producto " + codigo + " agregado al catálogo.");
        }
        // POST -> redirect -> GET (como en el carrito: F5 no lo graba dos veces)
        resp.sendRedirect(req.getContextPath() + "/admin-productos");
    }

    // ------------------------------------------------------------------
    // SQL
    // ------------------------------------------------------------------

    // Todos, tambien los dados de baja (al final)
    private List<Producto> listarTodos() throws SQLException {
        String sql = "SELECT " + Producto.COLUMNAS + " FROM productos ORDER BY activo DESC, categoria, nombre";
        List<Producto> lista = new ArrayList<>();
        try (Connection conn = ConexionBd.obtenerConexion();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                lista.add(Producto.desdeFila(rs));
            }
        }
        return lista;
    }

    private void insertar(String codigo, Map<String, String> form, long precio, int stock, int stockMinimo,
                          byte[] imagen, String tipo) throws SQLException {
        String sql = "INSERT INTO productos (codigo, nombre, descripcion, marca, categoria, "
                   + "precio_venta, stock, stock_minimo, imagen, imagen_tipo) "
                   + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = ConexionBd.obtenerConexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, codigo);
            ps.setString(2, form.get("nombre"));
            ps.setString(3, form.get("descripcion"));
            ps.setString(4, form.get("marca"));
            ps.setString(5, form.get("categoria"));
            ps.setLong(6, precio);
            ps.setInt(7, stock);
            ps.setInt(8, stockMinimo);
            ps.setBytes(9, imagen);   // setBytes: guarda la foto (o NULL si no eligieron)
            ps.setString(10, tipo);
            ps.executeUpdate();
        }
    }

    /*
     * Modificar. La foto solo se cambia si eligieron una nueva; si no, queda la que estaba.
     * Por eso el SQL se arma en dos partes (como los filtros del catalogo).
     */
    private void modificar(int id, Map<String, String> form, long precio, int stock, int stockMinimo,
                           byte[] imagen, String tipo) throws SQLException {
        String sql = "UPDATE productos SET nombre = ?, descripcion = ?, marca = ?, categoria = ?, "
                   + "precio_venta = ?, stock = ?, stock_minimo = ?";
        if (imagen != null) {
            sql = sql + ", imagen = ?, imagen_tipo = ?";
        }
        sql = sql + " WHERE id = ?";

        try (Connection conn = ConexionBd.obtenerConexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, form.get("nombre"));
            ps.setString(2, form.get("descripcion"));
            ps.setString(3, form.get("marca"));
            ps.setString(4, form.get("categoria"));
            ps.setLong(5, precio);
            ps.setInt(6, stock);
            ps.setInt(7, stockMinimo);
            int siguiente = 8;
            if (imagen != null) {
                ps.setBytes(8, imagen);
                ps.setString(9, tipo);
                siguiente = 10;
            }
            ps.setInt(siguiente, id);
            ps.executeUpdate();
        }
    }

    private void cambiarActivo(String codigo, boolean activo) throws SQLException {
        String sql = "UPDATE productos SET activo = ? WHERE codigo = ?";
        try (Connection conn = ConexionBd.obtenerConexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setBoolean(1, activo);
            ps.setString(2, codigo);
            ps.executeUpdate();
        }
    }

    // ------------------------------------------------------------------
    // Ayudas
    // ------------------------------------------------------------------

    /*
     * Que tipo de imagen es, mirando sus PRIMEROS BYTES (su "firma").
     * No confiamos en el nombre del archivo ni en lo que dice el navegador:
     * cualquiera puede llamar "foto.png" a un archivo que no es una imagen.
     * Cada formato empieza siempre igual:
     *     PNG  -> 89 50 4E 47        (las letras "PNG")
     *     JPG  -> FF D8 FF
     *     WEBP -> "RIFF" .... "WEBP"
     */
    private String tipoDeImagen(byte[] b) {
        if (b.length > 12) {
            if ((b[0] & 0xFF) == 0x89 && b[1] == 'P' && b[2] == 'N' && b[3] == 'G') {
                return "image/png";
            }
            if ((b[0] & 0xFF) == 0xFF && (b[1] & 0xFF) == 0xD8 && (b[2] & 0xFF) == 0xFF) {
                return "image/jpeg";
            }
            if (b[0] == 'R' && b[1] == 'I' && b[2] == 'F' && b[3] == 'F'
                    && b[8] == 'W' && b[9] == 'E' && b[10] == 'B' && b[11] == 'P') {
                return "image/webp";
            }
        }
        return null;   // no es ninguna de las tres
    }

    // Solo letras, numeros y guion
    private boolean esCodigoValido(String codigo) {
        for (char c : codigo.toCharArray()) {
            if (!Character.isLetterOrDigit(c) && c != '-') {
                return false;
            }
        }
        return true;
    }

    /*
     * Convierte el texto en numero. Acepta "1.850.000" (le saca los puntos).
     * Si no es un numero entero >= 0, agrega el error y devuelve 0.
     */
    private long leerNumero(String texto, String campo, List<String> errores) {
        try {
            long numero = Long.parseLong(texto.replace(".", ""));
            if (numero < 0 || numero > Integer.MAX_VALUE) {
                errores.add(campo + " tiene que ser 0 o más.");
                return 0;
            }
            return numero;
        } catch (NumberFormatException e) {
            errores.add(campo + " tiene que ser un número entero.");
            return 0;
        }
    }

    private void mostrarFormulario(HttpServletRequest req, HttpServletResponse resp, Map<String, String> form,
                                   List<String> errores, boolean editando)
            throws ServletException, IOException {
        try {
            req.setAttribute("categorias", CatalogoServlet.listarCategorias());   // para sugerir
        } catch (SQLException e) {
            req.setAttribute("categorias", new ArrayList<String>());
        }
        req.setAttribute("form", form);
        req.setAttribute("errores", errores);
        req.setAttribute("editando", editando);
        getServletContext().getRequestDispatcher("/producto-form.jsp").forward(req, resp);
    }

    /*
     * true si es ADMIN. Si no, ya lo manda a otro lado y devuelve false
     * (por eso en doGet/doPost: if (!esAdmin(...)) return;).
     */
    private boolean esAdmin(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        HttpSession sesion = req.getSession(false);
        if (sesion == null || sesion.getAttribute("usuario") == null) {
            resp.sendRedirect(req.getContextPath() + "/login.jsp");
            return false;
        }
        if (!"ADMIN".equals(sesion.getAttribute("rol"))) {
            resp.sendRedirect(req.getContextPath() + "/catalogo");
            return false;
        }
        return true;
    }
}
