-- Se ejecuta SOLO la primera vez que se crea el volumen de MySQL.
-- Un solo contenedor MySQL, pero cada microservicio tiene SU propia base de datos.
-- Las tablas las crea cada servicio con JPA (ddl-auto=update) y carga los datos demo.

CREATE DATABASE IF NOT EXISTS auth_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS liquidador_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- El usuario de la app (MYSQL_USER) lo crea la imagen de MySQL; aqui solo se le dan permisos
GRANT ALL PRIVILEGES ON auth_db.* TO 'bpo_app'@'%';
GRANT ALL PRIVILEGES ON liquidador_db.* TO 'bpo_app'@'%';
FLUSH PRIVILEGES;
