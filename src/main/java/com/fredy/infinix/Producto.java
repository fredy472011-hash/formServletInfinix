package com.fredy.infinix;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.NumberFormat;
import java.util.Locale;

/*
 * Un producto del catalogo. Es una fila de la tabla "productos" convertida en objeto.
 *
 * Solo tiene datos y getters: no hace nada mas.
 * Lo usan el catalogo, el detalle, el carrito, el ABM y el reporte de stock.
 */
public class Producto {

    /*
     * Las columnas que leemos SIEMPRE, en todos los SELECT de productos:
     *     "SELECT " + Producto.COLUMNAS + " FROM productos WHERE ..."
     *
     * Por que no "SELECT *": la columna "imagen" tiene la foto entera (bytes). Si la
     * traemos en el catalogo, por cada producto viajaria la foto completa sin usarla.
     * En su lugar preguntamos solo SI tiene foto:  imagen IS NOT NULL  -> 1 o 0.
     * La foto en si la manda ImagenServlet, aparte.
     */
    public static final String COLUMNAS =
            "id, codigo, nombre, descripcion, marca, categoria, precio_venta, stock, stock_minimo, activo, "
          + "imagen IS NOT NULL AS tiene_imagen";

    private final int id;
    private final String codigo;
    private final String nombre;
    private final String descripcion;
    private final String marca;
    private final String categoria;
    private final long precioVenta;   // en guaranies
    private final int stock;
    private final int stockMinimo;    // debajo de esto hay que reponer
    private final boolean activo;     // false = dado de baja en el ABM (no se vende mas)
    private final boolean tieneImagen;

    public Producto(int id, String codigo, String nombre, String descripcion, String marca,
                    String categoria, long precioVenta, int stock, int stockMinimo,
                    boolean activo, boolean tieneImagen) {
        this.id = id;
        this.codigo = codigo;
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.marca = marca;
        this.categoria = categoria;
        this.precioVenta = precioVenta;
        this.stock = stock;
        this.stockMinimo = stockMinimo;
        this.activo = activo;
        this.tieneImagen = tieneImagen;
    }

    /*
     * Arma un Producto con la fila actual de un SELECT.
     * Esta aca para no repetir estas lineas en cada servlet que lee productos.
     * El SELECT tiene que traer Producto.COLUMNAS.
     */
    public static Producto desdeFila(ResultSet rs) throws SQLException {
        return new Producto(
                rs.getInt("id"),
                rs.getString("codigo"),
                rs.getString("nombre"),
                rs.getString("descripcion"),
                rs.getString("marca"),
                rs.getString("categoria"),
                rs.getLong("precio_venta"),
                rs.getInt("stock"),
                rs.getInt("stock_minimo"),
                rs.getBoolean("activo"),
                rs.getBoolean("tiene_imagen"));
    }

    // Viene del diagrama de clases: Producto.hayStock()
    public boolean hayStock() {
        return stock > 0;
    }

    // Viene del diagrama de clases: Producto.necesitaReposicion()
    public boolean necesitaReposicion() {
        return stock < stockMinimo;
    }

    /*
     * La direccion de la foto, para poner en <img src="...">.
     * Si tiene foto, la manda ImagenServlet. Si no, un dibujo generico.
     */
    public String getUrlImagen() {
        if (tieneImagen) {
            return "imagen?codigo=" + codigo;
        }
        return "img/sin-imagen.svg";
    }

    // El precio con puntos de miles, como se escribe en Paraguay: 1850000 -> "1.850.000"
    public String getPrecioConPuntos() {
        return conPuntos(precioVenta);
    }

    /*
     * Pone los puntos de miles a cualquier monto: 2090000 -> "2.090.000".
     * NumberFormat sabe poner los puntos segun el pais (Locale "es", "PY").
     * Es static: se usa sin tener un producto, por ejemplo Producto.conPuntos(total).
     */
    public static String conPuntos(long monto) {
        return NumberFormat.getInstance(Locale.of("es", "PY")).format(monto);
    }

    public int getId()             { return id; }
    public String getCodigo()      { return codigo; }
    public String getNombre()      { return nombre; }
    public String getDescripcion() { return descripcion == null ? "" : descripcion; }
    public String getMarca()       { return marca; }
    public String getCategoria()   { return categoria; }
    public long getPrecioVenta()   { return precioVenta; }
    public int getStock()          { return stock; }
    public int getStockMinimo()    { return stockMinimo; }
    public boolean isActivo()      { return activo; }
    public boolean tieneImagen()   { return tieneImagen; }
}
