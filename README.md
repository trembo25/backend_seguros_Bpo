# Seguros BPO – Microservicios v2

Backend de la app MiRegistro / Seguros BPO. Seis proyectos Spring Boot 4.0.9 (Java 21, Spring Cloud 2025.1.4), un solo contenedor MySQL 8.4 y Swagger en todos los servicios.

## Estructura

```
microServiciosBPO_v2/
├── docker-compose.yml        Levanta todo (MySQL + Eureka + 4 servicios + gateway)
├── .env / .env.example       Claves de MySQL y JWT_SECRET
├── mysql-init/init.sql       Crea auth_db y liquidador_db (solo la primera vez)
├── postman/                  Colección v2 (57 requests con validaciones)
├── eureka-server/            8761  Registro de servicios
├── api-gateway/              8090  Puerta única + Swagger unificado
├── auth-service/             8081  Login, registro, recuperar, usuarios   (auth_db)
├── usuario-service/          8083  Asegurado: perfil y sus siniestros     (sin BD)
├── liquidador-service/       8084  Dueño de los siniestros                (liquidador_db)
└── analista-service/         8085  Panel, filtros y reasignación          (sin BD)
```

Cada servicio tiene su `Dockerfile` (multi-stage: Maven para compilar, JRE 21 para ejecutar), su `pom.xml` y sus tests.

## Cómo se comunican

```
App Android ──► api-gateway:8090 ──(lb:// por Eureka)──► servicio
                                                          │
usuario-service ──Feign──► auth-service (/auth/me)        │
usuario-service ──Feign──► liquidador-service (consultas del asegurado)
analista-service ─Feign──► auth-service (/auth/usuarios?rol=LIQUIDADOR)
analista-service ─Feign──► liquidador-service (consultas y reasignación)
liquidador-service ─Feign► auth-service (/auth/usuarios/{id})
```

- Feign ya no usa URL fija: busca cada servicio por su nombre en Eureka.
- El token JWT se reenvía tal cual y cada servicio vuelve a validar el rol.
- Todos los errores tienen el mismo formato: `{"error": "mensaje"}`.

## Levantar con Docker

```bash
docker compose up --build
```

Orden de arranque: MySQL (healthcheck) → Eureka (healthcheck) → auth / liquidador / usuario / analista → gateway. La primera vez tarda porque descarga dependencias de Maven. Después de que todo arranca, espera unos 15–30 segundos a que los servicios aparezcan en Eureka antes de probar desde la app.

Para empezar de cero (borra la base de datos):

```bash
docker compose down -v
```

Importante: `mysql-init/init.sql` da permisos al usuario `bpo_app`. Si cambias `DB_USER` en `.env`, cambia también ese nombre en `init.sql`.

## URLs

| Qué | URL |
|---|---|
| Gateway (la app usa esta) | http://localhost:8090 |
| App en emulador Android | http://10.0.2.2:8090/ |
| Swagger unificado | http://localhost:8090/swagger-ui.html (selector arriba a la derecha) |
| Panel de Eureka | http://localhost:8761 |
| Swagger directo de cada servicio | http://localhost:8081/swagger-ui.html, 8083, 8084, 8085 |
| MySQL desde tu PC | localhost:3307 (usuario `bpo_app`) |

En Swagger: haz login en `POST /auth/login`, copia el `token`, presiona Authorize y pégalo (sin escribir "Bearer").

## Usuarios demo

Contraseña de todos: `Clave123`. Respuestas de seguridad: `demo`.

| id | Nombre | Correo | Rol |
|---|---|---|---|
| 1 | Camila Rojas | camila.rojas@correo.ejemplo | ASEGURADO |
| 2 | Jorge Pizarro | jorge.pizarro@correo.ejemplo | ASEGURADO |
| 3 | Valentina Soto | vale.soto@correo.ejemplo | ASEGURADO |
| 4 | Paulina Fernández | paulina.fernandez@andes-bpo.ejemplo | LIQUIDADOR |
| 5 | Matías Gallardo | matias.gallardo@andes-bpo.ejemplo | LIQUIDADOR |
| 6 | Natalia Reyes | natalia.reyes@andes-bpo.ejemplo | ANALISTA |

## Endpoints (todos por el gateway)

### auth-service – `/auth`
| Método | Ruta | Quién |
|---|---|---|
| POST | /auth/registro | público (crea ASEGURADO) |
| POST | /auth/login | público |
| POST | /auth/recuperar/validar | público |
| POST | /auth/recuperar/nueva-contrasena | público (con resetToken) |
| PUT | /auth/cambiar-contrasena | con sesión |
| GET | /auth/me | con sesión |
| GET | /auth/usuarios/{id} | asegurado (solo él), liquidador, analista |
| GET | /auth/usuarios?rol=LIQUIDADOR | **nuevo**, solo ANALISTA |

### usuario-service – `/asegurado` (solo ASEGURADO, nuevo)
| Método | Ruta | Qué hace |
|---|---|---|
| GET | /asegurado/perfil | datos del asegurado |
| GET | /asegurado/resumen | perfil + conteo por estado + documentos pendientes |
| GET | /asegurado/siniestros | mis siniestros |
| GET | /asegurado/siniestros/{numero} | detalle (403 si no es suyo) |

### liquidador-service – `/liquidador`
| Método | Ruta | Quién |
|---|---|---|
| GET | /liquidador/siniestros?estado= | LIQUIDADOR |
| GET | /liquidador/siniestros/{numero} | LIQUIDADOR |
| POST | /liquidador/siniestros | LIQUIDADOR |
| PUT | /liquidador/siniestros/{numero}/estado | LIQUIDADOR |
| POST | /liquidador/siniestros/{numero}/documentos | LIQUIDADOR |
| PUT | /liquidador/siniestros/{numero}/documentos/{id}/recibido | LIQUIDADOR |
| DELETE | /liquidador/siniestros/{numero}/documentos/{id} | LIQUIDADOR |
| GET | /liquidador/consultas/asegurado/siniestros[/{numero}] | **nuevo**, ASEGURADO (lo usa usuario-service) |
| GET | /liquidador/consultas/analista/siniestros[/{numero}]?estado= | **nuevo**, ANALISTA (lo usa analista-service) |
| PUT | /liquidador/consultas/analista/siniestros/{numero}/liquidador | **nuevo**, ANALISTA |

### analista-service – `/analista` (solo ANALISTA, nuevo; reemplaza a admin-service)
| Método | Ruta | Qué hace |
|---|---|---|
| GET | /analista/panel?estado=TODOS | métricas + lista filtrada + liquidadores con carga |
| GET | /analista/siniestros?estado= | lista con nombre del liquidador |
| GET | /analista/siniestros/{numero} | detalle |
| GET | /analista/liquidadores | liquidadores con siniestros activos y totales |
| PUT | /analista/siniestros/{numero}/asignar | body `{"idLiquidador": 5}` |

Estados válidos: `EN_REVISION`, `DOCUMENTACION_PENDIENTE`, `EN_LIQUIDACION`, `LIQUIDADO`, `RECHAZADO` (y `TODOS` en los filtros del analista). Un siniestro `LIQUIDADO` o `RECHAZADO` no se puede reasignar (409).

## Tests

Dentro de cada carpeta:

```bash
./mvnw test        # Windows: mvnw.cmd test
```

No necesitan MySQL, Eureka ni Docker (JUnit 5 + Mockito + MockMvc standalone).

| Servicio | Tests |
|---|---|
| eureka-server | 3 (levanta Eureka en puerto aleatorio) |
| api-gateway | 6 (rutas lb:// y enrutamiento real con servidor falso) |
| auth-service | 50 |
| usuario-service | 13 |
| liquidador-service | 61 |
| analista-service | 16 |

## Probar un servicio sin Docker

- auth-service y liquidador-service: `./mvnw spring-boot:run -Dspring-boot.run.profiles=h2` (H2 en memoria, sin Eureka).
- usuario-service y analista-service: `./mvnw spring-boot:run -Dspring-boot.run.profiles=local` (sin Eureka, apuntan a localhost:8081 y 8084).
- El `JWT_SECRET` debe ser el mismo en todos.
# backend_seguros_Bpo
