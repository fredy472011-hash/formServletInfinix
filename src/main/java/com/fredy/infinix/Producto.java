package com.fredy.infinix;

import java.text.NumberFormat;
import java.util.Locale;

/*
 * Un producto del catalogo. Es una fila de la tabla "productos" convertida en objeto.
 *
 * Solo tiene datos y getters: no hace nada mas.
 * CatalogoServlet crea uno por cada fila, y catalogo.jsp los muestra.
 */
public class Producto {

    private final String codigo;
    private final String nombre;
    private final String descripcion;
    private final String marca;
    private final String categoria;
    private final long precioVenta;   // en guaranies
    private final int stock;

    public Producto(String codigo, String nombre, String descripcion, String marca,
                    String categoria, long precioVenta, int stock) {
        this.codigo = codigo;
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.marca = marca;
        this.categoria = categoria;
        this.precioVenta = precioVenta;
        this.stock = stock;
    }

    // Viene del diagrama de clases: Producto.hayStock()
    public boolean hayStock() {
        return stock > 0;
    }

    // El precio con puntos de miles, como se escribe en Paraguay: 1850000 -> "1.850.000"
    // NumberFormat sabe poner los puntos segun el pais (Locale "es", "PY").
    public String getPrecioConPuntos() {
        return NumberFormat.getInstance(Locale.of("es", "PY")).format(precioVenta);
    }

    public String getCodigo()      { return codigo; }
    public String getNombre()      { return nombre; }
    public String getDescripcion() { return descripcion; }
    public String getMarca()       { return marca; }
    public String getCategoria()   { return categoria; }
    public long getPrecioVenta()   { return precioVenta; }
    public int getStock()          { return stock; }
}
