-- Base de datos de Infinix Tech
-- Uso (PowerShell, desde la carpeta del proyecto):
--     cmd /c "mysql -u root -p < schema.sql"
--
-- Las columnas salen del diagrama de clases (Usuario, Producto, Venta, DetalleVenta),
-- y el nombre de la base del diagrama de despliegue (bd_infinixtech).

CREATE DATABASE IF NOT EXISTS bd_infinixtech CHARACTER SET utf8mb4;
USE bd_infinixtech;

-- Quien puede entrar al sistema
CREATE TABLE IF NOT EXISTS usuarios (
  id          INT AUTO_INCREMENT PRIMARY KEY,
  usuario     VARCHAR(50)  NOT NULL UNIQUE,      -- UNIQUE: no puede haber dos iguales
  contrasenha VARCHAR(100) NOT NULL,             -- guarda el HASH, nunca la contrasenha real
  email       VARCHAR(100) NOT NULL,
  rol         VARCHAR(20)  NOT NULL DEFAULT 'CLIENTE',   -- CLIENTE o ADMIN
  activo      BOOLEAN      NOT NULL DEFAULT TRUE
) ENGINE=InnoDB;

-- Lo que vende la tienda
CREATE TABLE IF NOT EXISTS productos (
  id           INT AUTO_INCREMENT PRIMARY KEY,
  codigo       VARCHAR(20)  NOT NULL UNIQUE,
  nombre       VARCHAR(100) NOT NULL,
  descripcion  VARCHAR(255),
  marca        VARCHAR(50)  NOT NULL,
  categoria    VARCHAR(50)  NOT NULL,
  precio_venta BIGINT       NOT NULL,            -- en guaranies, sin decimales
  stock        INT          NOT NULL DEFAULT 0,
  stock_minimo INT          NOT NULL DEFAULT 5,  -- debajo de esto, hay que reponer
  activo       BOOLEAN      NOT NULL DEFAULT TRUE,   -- baja logica: FALSE = no se vende mas
  imagen       MEDIUMBLOB,                       -- la foto, en bytes (hasta 16 MB; el sistema acepta 2 MB)
  imagen_tipo  VARCHAR(30)                       -- image/png, image/jpeg o image/webp
) ENGINE=InnoDB;

-- Una compra (la cabecera). Diagrama: clase Venta
CREATE TABLE IF NOT EXISTS ventas (
  id         INT AUTO_INCREMENT PRIMARY KEY,
  id_usuario INT         NOT NULL,             -- quien compro
  fecha      DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  total      BIGINT      NOT NULL,
  estado     VARCHAR(20) NOT NULL DEFAULT 'CONFIRMADA',
  medio_pago VARCHAR(20) NOT NULL DEFAULT 'EFECTIVO',  -- DEBITO, CREDITO o QR. Diagrama: MedioPago
  ultimos_digitos CHAR(4),                             -- solo los ultimos 4 de la tarjeta, nunca el numero entero
  cuotas     INT         NOT NULL DEFAULT 1,           -- diagrama: CondicionVenta.cantidadCuotas
  FOREIGN KEY (id_usuario) REFERENCES usuarios(id)
) ENGINE=InnoDB;

-- Cada producto de una compra (las lineas). Diagrama: clase DetalleVenta
--   una venta tiene 1 o mas detalles; cada detalle es de 1 producto
CREATE TABLE IF NOT EXISTS detalle_ventas (
  id              INT AUTO_INCREMENT PRIMARY KEY,
  id_venta        INT    NOT NULL,
  id_producto     INT    NOT NULL,
  cantidad        INT    NOT NULL,
  precio_unitario BIGINT NOT NULL,             -- el precio de ESE dia (si despues cambia, no importa)
  subtotal        BIGINT NOT NULL,
  FOREIGN KEY (id_venta)    REFERENCES ventas(id),
  FOREIGN KEY (id_producto) REFERENCES productos(id)
) ENGINE=InnoDB;

-- Productos de ejemplo. INSERT IGNORE: si ya existen (mismo codigo), no los repite.
INSERT IGNORE INTO productos (codigo, nombre, descripcion, marca, categoria, precio_venta, stock, stock_minimo) VALUES
('CEL-001', 'Infinix Note 40',      '8 GB de RAM, 256 GB, pantalla AMOLED 6,78"', 'Infinix',  'Celulares',   1850000, 12, 5),
('CEL-002', 'Infinix Hot 50',       '6 GB de RAM, 128 GB, bateria de 5000 mAh',    'Infinix',  'Celulares',   1150000, 20, 5),
('CEL-003', 'Samsung Galaxy A35',   '8 GB de RAM, 256 GB, camara de 50 MP',        'Samsung',  'Celulares',   2490000,  7, 5),
('NOT-001', 'Lenovo IdeaPad Slim 3','Ryzen 5, 16 GB de RAM, SSD de 512 GB',        'Lenovo',   'Notebooks',   4600000,  5, 3),
('NOT-002', 'HP 15',                'Core i5, 8 GB de RAM, SSD de 512 GB',         'HP',       'Notebooks',   4150000,  0, 3),
('AUD-001', 'Auriculares JBL Tune 520BT', 'Bluetooth, 57 horas de bateria',        'JBL',      'Accesorios',   420000, 25, 10),
('ACC-001', 'Cargador rapido 33 W', 'USB-C, compatible con Infinix y Samsung',     'Infinix',  'Accesorios',   120000, 40, 10),
('TV-001',  'Smart TV 43" 4K',      'Android TV, Wi-Fi, 3 HDMI',                   'TCL',      'Televisores', 2350000,  3, 2);

-- Teclados, como los que vende InfinixTec (precios de ejemplo: confirmarlos con la tienda)
INSERT IGNORE INTO productos (codigo, nombre, descripcion, marca, categoria, precio_venta, stock, stock_minimo) VALUES
('TEC-001', 'Teclado gamer Rainbow Glow',     'Membrana, retroiluminado RGB, USB',                  'Genérico',  'Teclados',  95000, 15, 5),
('TEC-002', 'Redragon Kumara K552 RGB',       'Mecánico sin numérico (TKL), switch rojo, RGB',      'Redragon',  'Teclados', 290000,  8, 3),
('TEC-003', 'Redragon Fizz K617 60%',         'Mecánico compacto 60 %, RGB, cable USB-C',           'Redragon',  'Teclados', 260000,  6, 3),
('TEC-004', 'Aula F75 inalámbrico',           'Mecánico 75 %, Bluetooth y 2,4 GHz, blanco y naranja','Aula',     'Teclados', 420000,  4, 2),
('TEC-005', 'Logitech K120',                  'Teclado de oficina USB, resistente a salpicaduras',  'Logitech',  'Teclados',  85000, 20, 5),
('TEC-006', 'Combo teclado y mouse gamer RGB','Teclado de membrana + mouse 3200 DPI, retroiluminados','Genérico', 'Teclados', 150000, 10, 4);

-- Para hacer ADMIN a un usuario (puede ver los reportes):
--     UPDATE usuarios SET rol = 'ADMIN' WHERE usuario = 'tu_usuario';
