package com.fredy.infinix;

/*
 * Una fila del carrito: QUE producto y CUANTOS.
 *
 *     Infinix Note 40   x 2   =  3.700.000
 *     Cargador 33 W     x 1   =    120.000
 *
 * El carrito entero es una List<ItemCarrito> guardada en la SESION
 * (ver CarritoServlet). Asi el carrito se mantiene mientras navegas.
 */
public class ItemCarrito {

    private final Producto producto;
    private int cantidad;   // no es final: cambia con los botones + y -

    public ItemCarrito(Producto producto, int cantidad) {
        this.producto = producto;
        this.cantidad = cantidad;
    }

    // precio x cantidad. Diagrama de clases: DetalleVenta.calcularSubtotal()
    public long getSubtotal() {
        return producto.getPrecioVenta() * cantidad;
    }

    public Producto getProducto() { return producto; }
    public int getCantidad()      { return cantidad; }

    public void setCantidad(int cantidad) {
        this.cantidad = cantidad;
    }
}
