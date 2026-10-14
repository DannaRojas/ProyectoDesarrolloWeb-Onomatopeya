# Onomatopeya

Backend del visor y editor de procesos empresariales. Las peticiones REST llegan a los controladores; los servicios aplican las reglas del negocio y los repositorios JPA consultan PostgreSQL. Las entidades representan el modelo persistente y los DTO definen los datos de entrada y salida.

## Requisitos

- Java 17 o superior.
- PostgreSQL con la base `gestion_procesos` y el esquema del proyecto.

## Configuración

La aplicación toma la conexión de `DB_URL`, `DB_USERNAME` y `DB_PASSWORD`. Si no se definen las dos primeras, usa los valores indicados en `src/main/resources/application.properties`. Nunca se debe incluir la contraseña en el repositorio.

Antes de usar una base compartida, configurar `spring.jpa.hibernate.ddl-auto=validate` y dejar desactivados los datos de demostración. Las migraciones y las pruebas de integridad SQL están en `database/`.

## Pruebas y documentación

Las pruebas automatizadas usan PostgreSQL 14 en bases aisladas, nunca la base compartida ni `gestion_procesos`. En Windows, crear primero `.env` con `DB_PASSWORD` y ejecutar desde la raíz:

```powershell
docker compose -p onoma-entrega2-check --profile pruebas up -d db_pruebas
$env:DB_PASSWORD = ((Get-Content .env | Where-Object { $_ -match '^DB_PASSWORD=' } | Select-Object -First 1) -replace '^DB_PASSWORD=', '').Trim('"').Trim("'")
.\mvnw.cmd clean verify
Remove-Item Env:DB_PASSWORD
```

El contenedor de pruebas escucha solo en `127.0.0.1:5433`; `create-drop` afecta únicamente las bases de ese contenedor. Con la aplicación en marcha, Swagger UI está en `/swagger-ui/index.html` y el documento OpenAPI en `/v3/api-docs`.

La aplicación aún no implementa el inicio de sesión definitivo. Los endpoints que requieren usuario esperan un `Principal`; esto debe resolverse antes de usarlos desde el frontend con usuarios reales.

## Demostración de API

Ejecutar `postman/IniciarDemo.ps1` en PowerShell y dejar esa terminal abierta. La demo usa la base PostgreSQL aislada `demo_postman` y el puerto 8084; no se conecta a PostgreSQL de Laura. Importar `postman/Entrega2.postman_collection.json` en Postman y ejecutar las cinco peticiones en orden. Se comprueban Swagger, el rechazo sin sesión, el registro y el NIT duplicado.

El plan `jmeter/Carga-API-local.jmx` hace tres cargas secuenciales de lectura sobre `/v3/api-docs`: 1, 10 y 30 usuarios. Usa el puerto 8084 de la demo PostgreSQL por defecto. Con Docker local en marcha se puede ejecutar `jmeter -n -t jmeter/Carga-API-local.jmx -Jport=8080 -l resultados-docker.jtl`. Estas cargas miden la documentación HTTP, no las consultas a la base ni usuarios autenticados.

Para medir escrituras reales en PostgreSQL, con la demo del puerto 8084 abierta ejecutar `jmeter -n -t jmeter/Registro-Empresas-Postgres.jmx -l resultados-registro.jtl`. Este plan registra 20 empresas en `demo_postman` (10 usuarios, dos vueltas) y espera HTTP 201. Solo apunta a `127.0.0.1:8084`; no usarlo con la base compartida.

## Análisis con SonarQube

`mvnw.cmd clean verify` genera el informe de cobertura en `target/site/jacoco/jacoco.xml`. Con SonarQube disponible y `SONAR_TOKEN` configurado en la terminal, ejecutar `mvnw.cmd org.sonarsource.scanner.maven:sonar-maven-plugin:5.8.0.7211:sonar -Dsonar.projectKey=onomatopeya -Dsonar.projectName=Onomatopeya -Dsonar.host.url=http://127.0.0.1:9000`. El token es personal y no debe guardarse en el repositorio.


## Docker local

Crear un archivo `.env` a partir de `.env.example` y poner una contraseña solo para la base local. Después ejecutar `docker compose up --build`. El contenedor de PostgreSQL guarda sus datos en un volumen local y la API queda en `http://127.0.0.1:8080`; no usa la base de Laura. `docker compose down` detiene los contenedores sin borrar ese volumen.

Este Compose cubre el backend y su base principal local. El servicio `db_pruebas` se inicia solo con el perfil `pruebas` y guarda sus datos en un volumen diferente. El frontend Angular se integrará cuando exista su repositorio; por ahora no representa la solución completa de la segunda entrega.
