# formServletInfinix

Sistema web de **Infinix Tech** (Ingeniería de Software II): login, registro, catálogo con filtros,
carrito, compra y reportes en PDF.

**Stack:** Java 21 · Jakarta EE 11 · Servlets · JSP · JDBC · MySQL 8 · Apache Tomcat 11 · Maven · OpenPDF.
Tiene la misma estructura que el proyecto `webapp-formFC`.

## Cómo funciona

```
login.jsp ──"Registrate"──► registro.jsp ──(cuenta creada)──► login.jsp
    │
    └──(usuario y contraseña bien)──► /catalogo ──► filtrar · buscar · ver detalle
                                          │
                                          └─ "Agregar al carrito" ──► /carrito ──► "Confirmar compra" ──► /comprar
                                                                                  (se graba la venta)
    Solo ADMIN: Reportes ──► /reporte-pdf?tipo=ventas   o   ?tipo=stock   (descarga un PDF)
```

| Archivo | Qué hace |
|---|---|
| `login.jsp`, `registro.jsp` | Ingresar y crear cuenta |
| `catalogo.jsp` | Productos en tarjetas, botones de categoría y buscador |
| `producto.jsp` | El detalle de un producto |
| `carrito.jsp` | El carrito con + / − / Quitar y Confirmar compra |
| `reportes.jsp` | Los dos botones de reportes (solo ADMIN) |
| `cabecera.jsp` | La barra azul de arriba, que se repite en todas las páginas internas |
| `RegistroServlet` (`/registro`) | Valida y guarda el usuario, con la contraseña en hash |
| `LoginServlet` (`/login`) | Controla usuario y contraseña y abre la sesión |
| `CatalogoServlet` (`/catalogo`) | Lee los productos, con filtro por categoría y búsqueda |
| `ProductoServlet` (`/producto`) | Busca un producto por su código |
| `CarritoServlet` (`/carrito`) | Maneja el carrito, que se guarda en la sesión |
| `CompraServlet` (`/comprar`) | Graba la venta, sus detalles y baja el stock, en una transacción |
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

## Lo nuevo respecto a webapp-formFC

1. **Hash de contraseña (BCrypt):** en la base no se guarda la contraseña, sino un resumen que no se puede revertir.
2. **Sesión (`HttpSession`):** la memoria del servidor sobre quién ingresó. También guarda el carrito.
3. **Redirect vs forward:** `forward` sirve para mostrar errores en la misma página; `redirect` lleva al navegador a otra dirección.
4. **Transacción:** la venta, sus detalles y el stock se graban juntos (`commit`), o nada (`rollback`).
5. **PDF con OpenPDF:** `Document`, `Paragraph` y `PdfPTable`.

## Cómo correrlo

1. **Base de datos** (una sola vez). En PowerShell, desde esta carpeta:
   ```powershell
   cmd /c "mysql -u root -p < schema.sql"
   ```
   Crea `bd_infinixtech` con las tablas `usuarios`, `productos`, `ventas` y `detalle_ventas`, y 8 productos de ejemplo.

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

7. **Para ver los reportes**, hacé ADMIN a tu usuario y volvé a ingresar:
   ```sql
   UPDATE bd_infinixtech.usuarios SET rol = 'ADMIN' WHERE usuario = 'tu_usuario';
   ```
