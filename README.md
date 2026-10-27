# formServletInfinix

Sistema web de **InfinixTec**, tienda de tecnología de Carapeguá (Ingeniería de Software II).
Tiene login, registro, catálogo con fotos y filtros, carrito, pago (débito, crédito o QR), un ABM de
productos con subida de imagen y reportes en PDF.

**Stack:** Java 21 · Jakarta EE 11 · Servlets · JSP · JDBC · MySQL 8 · Apache Tomcat 11 · Maven ·
OpenPDF · ZXing. Tiene la misma estructura que el proyecto `webapp-formFC`.

## Cómo funciona

```
login.jsp ──"Registrate"──► registro.jsp ──(cuenta creada)──► login.jsp
    │
    └──(usuario y contraseña bien)──► /catalogo ──► filtrar · buscar · ver detalle
                                          │
                                          └─ "Agregar al carrito" ──► /carrito ──► "Ir a pagar" ──► /pago
                                                                       débito / crédito / QR ──► /comprar
                                                                                         (se graba la venta)
    Solo ADMIN: Productos ──► /admin-productos   (alta, modificación, baja, foto)
                Reportes  ──► /reporte-pdf?tipo=ventas   o   ?tipo=stock   (descarga un PDF)
```

| Archivo | Qué hace |
|---|---|
| `login.jsp`, `registro.jsp` | Ingresar y crear cuenta |
| `catalogo.jsp` | Productos en tarjetas, botones de categoría y buscador |
| `producto.jsp` | El detalle de un producto |
| `carrito.jsp` | El carrito con + / − / Quitar e Ir a pagar |
| `pago.jsp` | Pestañas Débito / Crédito / QR, con el resumen del pedido |
| `admin-productos.jsp`, `producto-form.jsp` | La lista del ABM y el formulario con la foto (solo ADMIN) |
| `reportes.jsp` | Los dos botones de reportes (solo ADMIN) |
| `cabecera.jsp`, `pie.jsp` | La barra azul de arriba y el pie con WhatsApp, iguales en todas las páginas internas |
| `img/logo.svg`, `img/sin-imagen.svg` | El logo y el dibujo para productos sin foto |
| `RegistroServlet` (`/registro`) | Valida y guarda el usuario, con la contraseña en hash |
| `LoginServlet` (`/login`) | Controla usuario y contraseña y abre la sesión |
| `CatalogoServlet` (`/catalogo`) | Lee los productos, con filtro por categoría y búsqueda |
| `ProductoServlet` (`/producto`) | Busca un producto por su código |
| `CarritoServlet` (`/carrito`) | Maneja el carrito, que se guarda en la sesión |
| `PagoServlet` (`/pago`) | Valida la tarjeta (formato, Luhn, vencimiento) y "aprueba" el pago |
| `QrServlet` (`/qr`) | Dibuja el código QR con el total (PNG) |
| `CompraServlet` (`/comprar`) | Graba la venta con su medio de pago, sus detalles y baja el stock, en una transacción |
| `ProductoAdminServlet` (`/admin-productos`) | ABM de productos: alta, modificación, baja lógica y subida de la foto |
| `ImagenServlet` (`/imagen`) | Devuelve la foto de un producto, guardada en la base |
| `Html` | Escapa los textos (comillas, `<`, `>`) antes de ponerlos en la página |
| `ReportePdfServlet` (`/reporte-pdf`) | Arma los PDF de ventas y de stock |
| `SalirServlet` (`/salir`) | Cierra la sesión |
| `Producto`, `ItemCarrito` | Un producto, y un producto con su cantidad dentro del carrito |
| `ConexionBd` | La conexión a MySQL (credenciales por variables de entorno) |

## Relación con el diagrama de clases

| Clase del diagrama | En el sistema |
|---|---|
| `Usuario.loguearse()` | `LoginServlet.loguearse()` |
| `Producto.hayStock()`, `necesitaReposicion()` | Métodos de `Producto` |
| `Venta`, `DetalleVenta` | Tablas `ventas` y `detalle_ventas` |
| `Venta.calcularTotal()`, `confirmar()` | `CarritoServlet.calcularTotal()`, `CompraServlet.registrarVenta()` |
| `DetalleVenta.calcularSubtotal()` | `ItemCarrito.getSubtotal()` |
| `Reporte.exportarPdf()` | `ReportePdfServlet` |
| `MedioPago`, `CondicionVenta.cantidadCuotas` | Columnas `medio_pago`, `ultimos_digitos` y `cuotas` de `ventas`; `PagoServlet` |
| Actor "Pasarela de pagos" | Simulada en `PagoServlet` (en la vida real, Bancard) |

## Lo nuevo respecto a webapp-formFC

1. **Hash de contraseña (BCrypt):** en la base no se guarda la contraseña, sino un resumen que no se puede revertir.
2. **Sesión (`HttpSession`):** la memoria del servidor sobre quién ingresó. También guarda el carrito.
3. **Redirect vs forward:** `forward` sirve para mostrar errores en la misma página; `redirect` lleva al navegador a otra dirección.
4. **Transacción:** la venta, sus detalles y el stock se graban juntos (`commit`), o nada (`rollback`).
5. **PDF con OpenPDF:** `Document`, `Paragraph` y `PdfPTable`.
6. **Subir un archivo:** `enctype="multipart/form-data"`, `@MultipartConfig` y `req.getPart("imagen")`.
   El tipo de imagen se reconoce por sus primeros bytes, no por el nombre del archivo.
7. **Devolver bytes en vez de HTML:** la foto, el QR y el PDF cambian el `Content-Type`.
8. **Baja lógica:** dar de baja pone `activo = FALSE`; no se borra, porque hay ventas que apuntan al producto.
9. **Datos de tarjeta:** es una simulación. Solo se valida el formato y se guardan los últimos 4 números,
   nunca el número completo ni el CVV.

> **Tomcat 11 acepta como máximo 10 partes** en un formulario multipart. Por eso, en el ABM, `accion` e `id`
> viajan en la dirección (`admin-productos?accion=guardar&id=5`) y no como campos ocultos.

## Cómo correrlo

1. **Base de datos** (una sola vez). En PowerShell, desde esta carpeta:
   ```powershell
   cmd /c "mysql -u root -p < schema.sql"
   ```
   Crea `bd_infinixtech` con las tablas `usuarios`, `productos`, `ventas` y `detalle_ventas`, y 14 productos
   de ejemplo (6 son teclados). Los precios son de ejemplo: hay que confirmarlos con la tienda.

2. **Clave de MySQL:** si tu `root` no usa la clave `root`:
   ```powershell
   setx DB_PASSWORD "tu_clave"
   ```

3. **Tomcat 11** encendido, con `mysql-connector-j` en su carpeta `lib`.

4. **Usuario del manager de Tomcat** (una sola vez):
   ```powershell
   setx TOMCAT_USER "admin"
   setx TOMCAT_PASSWORD "tu_clave"
   ```
   Después de un `setx`, cerrá y volvé a abrir IntelliJ o la terminal.

5. **Compilar y subir a Tomcat:** en IntelliJ, elegí la configuración **tomcat** (arriba a la derecha) y
   apretá ▶. Viene en la carpeta `.run` del proyecto. Es lo mismo que escribir:
   ```powershell
   mvn package tomcat7:redeploy
   ```

6. Abrir <http://localhost:8080/formServletInfinix/>, crear una cuenta e ingresar.

7. **Para ver Productos (ABM) y Reportes**, hacé ADMIN a tu usuario y volvé a ingresar:
   ```sql
   UPDATE bd_infinixtech.usuarios SET rol = 'ADMIN' WHERE usuario = 'tu_usuario';
   ```
