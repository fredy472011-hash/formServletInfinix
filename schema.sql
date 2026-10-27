-- Base de datos de Infinix Tech
-- Uso (PowerShell, desde la carpeta del proyecto):
--     cmd /c "mysql -u root -p < schema.sql"
--
-- Las columnas salen del diagrama de clases (Usuario y Producto),
-- y el nombre de la base del diagrama de despliegue (bd_infinixtech).

CREATE DATABASE IF NOT EXISTS bd_infinixtech CHARACTER SET utf8mb4;
USE bd_infinixtech;

-- Quien puede entrar al sistema
CREATE TABLE IF NOT EXISTS usuarios (
  id          INT AUTO_INCREMENT PRIMARY KEY,
  usuario     VARCHAR(50)  NOT NULL UNIQUE,      -- UNIQUE: no puede haber dos iguales
  contrasenha VARCHAR(100) NOT NULL,             -- guarda el HASH, nunca la contrasenha real
  email       VARCHAR(100) NOT NULL,
  rol         VARCHAR(20)  NOT NULL DEFAULT 'CLIENTE',
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
  stock        INT          NOT NULL DEFAULT 0
) ENGINE=InnoDB;

-- Productos de ejemplo. INSERT IGNORE: si ya existen (mismo codigo), no los repite.
INSERT IGNORE INTO productos (codigo, nombre, descripcion, marca, categoria, precio_venta, stock) VALUES
('CEL-001', 'Infinix Note 40',      '8 GB de RAM, 256 GB, pantalla AMOLED 6,78"', 'Infinix',  'Celulares',   1850000, 12),
('CEL-002', 'Infinix Hot 50',       '6 GB de RAM, 128 GB, bateria de 5000 mAh',    'Infinix',  'Celulares',   1150000, 20),
('CEL-003', 'Samsung Galaxy A35',   '8 GB de RAM, 256 GB, camara de 50 MP',        'Samsung',  'Celulares',   2490000,  7),
('NOT-001', 'Lenovo IdeaPad Slim 3','Ryzen 5, 16 GB de RAM, SSD de 512 GB',        'Lenovo',   'Notebooks',   4600000,  5),
('NOT-002', 'HP 15',                'Core i5, 8 GB de RAM, SSD de 512 GB',         'HP',       'Notebooks',   4150000,  0),
('AUD-001', 'Auriculares JBL Tune 520BT', 'Bluetooth, 57 horas de bateria',        'JBL',      'Accesorios',   420000, 25),
('ACC-001', 'Cargador rapido 33 W', 'USB-C, compatible con Infinix y Samsung',     'Infinix',  'Accesorios',   120000, 40),
('TV-001',  'Smart TV 43" 4K',      'Android TV, Wi-Fi, 3 HDMI',                   'TCL',      'Televisores', 2350000,  3);
