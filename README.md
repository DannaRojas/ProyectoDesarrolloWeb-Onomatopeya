# Onomatopeya — Ejecución con Docker Compose

## Requisitos previos

- Docker Desktop instalado y en ejecución.
- Archivo `.env` local configurado en la raíz del proyecto.

## Configuración inicial

Antes de iniciar los servicios, crea el archivo `.env` a partir de `.env.example` si todavía no existe y configura las variables requeridas con valores locales propios.

No compartas ni subas el archivo `.env` al repositorio.

## Iniciar la aplicación

Desde la carpeta raíz, donde se encuentra `compose.yaml`, ejecuta:

```bash
docker compose up -d --build
```

Este comando construye las imágenes necesarias e inicia los servicios de PostgreSQL, API backend y frontend.

## Comprobar el estado

```bash
docker compose ps
```

La base de datos debe aparecer como `healthy`. Los servicios `api` y `frontend` deben estar en ejecución.

## Acceder a la aplicación

- Frontend: http://localhost:4200
- API backend: http://localhost:8080
- Prueba de comunicación frontend-backend: http://localhost:4200/api/empresas

La ruta de empresas devuelve los datos JSON disponibles en el backend.

## Consultar los registros

Para ver los registros de todos los servicios:

```bash
docker compose logs
```

Para seguir los registros en tiempo real:

```bash
docker compose logs -f
```

Pulsa `Ctrl+C` para salir de la visualización de registros.

## Detener la aplicación

```bash
docker compose down
```

Este comando detiene y elimina los contenedores de la aplicación, pero conserva los volúmenes de datos.

Para volver a iniciar la aplicación, ejecuta:

```bash
docker compose up -d
```

## Consideraciones

- No se deben guardar contraseñas ni secretos en las imágenes Docker ni en el repositorio.
- El archivo `.env` debe mantenerse local y fuera del control de versiones.
- No utilizar `docker compose down -v` si se desea conservar los datos de PostgreSQL.
