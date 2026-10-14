# Onomatopeya

Backend del visor y editor de procesos empresariales. Las peticiones REST llegan a los controladores; los servicios aplican las reglas del negocio y los repositorios JPA consultan PostgreSQL. Las entidades representan el modelo persistente y los DTO definen los datos de entrada y salida.

## Requisitos

- Java 17 o superior.
- PostgreSQL con la base `gestion_procesos` y el esquema del proyecto.

## Configuración

La aplicación toma la conexión de `DB_URL`, `DB_USERNAME` y `DB_PASSWORD`. Si no se definen las dos primeras, usa los valores indicados en `src/main/resources/application.properties`. Nunca se debe incluir la contraseña en el repositorio.

Antes de usar una base compartida, configurar `spring.jpa.hibernate.ddl-auto=validate` y dejar desactivados los datos de demostración. Las migraciones y las pruebas de integridad SQL están en `database/`.

## Pruebas y documentación

En Windows, ejecutar `mvnw.cmd clean verify`. Las pruebas automatizadas usan H2 en memoria y no modifican PostgreSQL. Con la aplicación en marcha, Swagger UI está en `/swagger-ui/index.html` y el documento OpenAPI en `/v3/api-docs`.

La aplicación aún no implementa el inicio de sesión definitivo. Los endpoints que requieren usuario esperan un `Principal`; esto debe resolverse antes de usarlos desde el frontend con usuarios reales.

## Demostración de API

Ejecutar `postman/IniciarDemo.ps1` en PowerShell y dejar esa terminal abierta. La demo usa H2 en memoria y el puerto 8084; no se conecta a PostgreSQL de Laura. Importar `postman/Entrega2.postman_collection.json` en Postman y ejecutar las cinco peticiones en orden. Se comprueban Swagger, el rechazo sin sesión, el registro y el NIT duplicado.


## Docker local

Crear un archivo `.env` a partir de `.env.example` y poner una contraseña solo para la base local. Después ejecutar `docker compose up --build`. El contenedor de PostgreSQL guarda sus datos en un volumen local y la API queda en `http://127.0.0.1:8080`; no usa la base de Laura. `docker compose down` detiene los contenedores sin borrar ese volumen.

Este Compose cubre el backend y una base de prueba. El frontend Angular se integrará cuando exista su repositorio; por ahora no representa la solución completa de la segunda entrega.
