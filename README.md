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
