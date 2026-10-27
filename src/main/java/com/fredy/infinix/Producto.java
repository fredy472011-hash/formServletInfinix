package com.fredy.infinix;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.NumberFormat;
import java.util.Locale;

/*
 * Un producto del catalogo. Es una fila de la tabla "productos" convertida en objeto.
 *
 * Solo tiene datos y getters: no hace nada mas.
 * Lo usan el catalogo, el detalle, el carrito y el reporte de stock.
 */
public class Producto {

    private final int id;
    private final String codigo;
    private final String nombre;
    private final String descripcion;
    private final String marca;
    private final String categoria;
    private final long precioVenta;   // en guaranies
    private final int stock;
    private final int stockMinimo;    // debajo de esto hay que reponer

    public Producto(int id, String codigo, String nombre, String descripcion, String marca,
                    String categoria, long precioVenta, int stock, int stockMinimo) {
        this.id = id;
        this.codigo = codigo;
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.marca = marca;
        this.categoria = categoria;
        this.precioVenta = precioVenta;
        this.stock = stock;
        this.stockMinimo = stockMinimo;
    }

    /*
     * Arma un Producto con la fila actual de un SELECT.
     * Esta aca para no repetir estas 9 lineas en cada servlet que lee productos.
     * El SELECT tiene que traer todas las columnas: SELECT * FROM productos ...
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
                rs.getInt("stock_minimo"));
    }

    // Viene del diagrama de clases: Producto.hayStock()
    public boolean hayStock() {
        return stock > 0;
    }

    // Viene del diagrama de clases: Producto.necesitaReposicion()
    public boolean necesitaReposicion() {
        return stock < stockMinimo;
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
    public String getDescripcion() { return descripcion; }
    public String getMarca()       { return marca; }
    public String getCategoria()   { return categoria; }
    public long getPrecioVenta()   { return precioVenta; }
    public int getStock()          { return stock; }
    public int getStockMinimo()    { return stockMinimo; }
}
