# formServletInfinix

Sistema web de **Infinix Tech** (Ingeniería de Software II): login, registro y catálogo de productos.

**Stack:** Java 21 · Jakarta EE 11 · Servlets · JSP · JDBC · MySQL 8 · Apache Tomcat 11 · Maven.
Tiene la misma estructura que el proyecto `webapp-formFC`.

## Cómo funciona

```
login.jsp ──"Registrate"──► registro.jsp ──(cuenta creada)──► login.jsp
    │
    └──(usuario y contraseña bien)──► /catalogo ──► catalogo.jsp
                                           └── "Salir" ──► login.jsp
```

| Archivo | Qué hace |
|---|---|
| `login.jsp` | Formulario de ingreso y link para registrarse |
| `registro.jsp` | Formulario para crear una cuenta |
| `catalogo.jsp` | Muestra los productos en tarjetas |
| `RegistroServlet` (`/registro`) | Valida y guarda el usuario, con la contraseña en hash |
| `LoginServlet` (`/login`) | Controla usuario y contraseña y abre la sesión |
| `CatalogoServlet` (`/catalogo`) | Solo con sesión: lee los productos y los manda al JSP |
| `SalirServlet` (`/salir`) | Cierra la sesión |
| `Producto` | Una fila de la tabla `productos` como objeto |
| `ConexionBd` | La conexión a MySQL (credenciales por variables de entorno) |

## Lo nuevo respecto a webapp-formFC

1. **Hash de contraseña (BCrypt):** en la base no se guarda la contraseña, sino un resumen que no se puede revertir.
2. **Sesión (`HttpSession`):** la memoria del servidor sobre quién ingresó.
3. **Redirect vs forward:** `forward` sirve para mostrar errores en la misma página; `redirect` lleva al navegador a otra dirección.

## Cómo correrlo

1. **Base de datos** (una sola vez). En PowerShell, desde esta carpeta:
   ```powershell
   cmd /c "mysql -u root -p < schema.sql"
   ```
   Crea `bd_infinixtech` con las tablas `usuarios` y `productos`, y 8 productos de ejemplo.

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
