# Integración por partes

Esta rama parte de `8ddb5ad`. Los archivos que ya estaban en esa versión se conservan sin cambios.

## Primera conexión: Empresa

Los archivos de `integracion/empresa` reciben y validan peticiones, calculan el hash del administrador y llaman al `EmpresaService` existente. La transacción de Laura guarda la empresa y su administrador. No se expone eliminación de empresas.

Pruebas en una base H2 temporal, sin conectarse a la máquina de Laura:

```powershell
.\mvnw.cmd -f pom-integracion.xml test
```

El archivo Maven auxiliar permite agregar dependencias y configurar Lombok sin cambiar el `pom.xml` del equipo. Hay que indicar `-f pom-integracion.xml` al compilar esta rama. No se debe ejecutar el proyecto con el POM original mientras esta integración dependa de las nuevas librerías.

Para PostgreSQL, primero debe estar activo el túnel universitario y el túnel SSH. Configurar `DB_URL`, `DB_USERNAME` y `DB_PASSWORD` como variables de entorno, sin guardarlas en Git. El perfil verifica las tablas existentes con `ddl-auto=validate`; no crea ni actualiza el esquema compartido.

```powershell
.\mvnw.cmd -f pom-integracion.xml spring-boot:run '-Dspring-boot.run.profiles=conexion-empresa'
```

Escucha en `http://127.0.0.1:8081`. La conexión necesita el esquema completo del modelo actual; si falta una tabla, el arranque se detiene para coordinar la migración con Laura.

### Peticiones

`POST /empresas`:

```json
{
  "empresa": {"nombre": "Empresa de prueba", "nit": "900123456", "correoContacto": "contacto@ejemplo.co"},
  "administrador": {"nombre": "Administrador", "correo": "admin@ejemplo.co", "contrasena": "CambiarClave123!"}
}
```

`GET /empresas`, `GET /empresas/{id}` y `PUT /empresas/{id}` llaman a las consultas y actualización del servicio existente. El PUT recibe solamente los tres campos de empresa. Los listados mantienen el comportamiento actual del servicio, incluidas las empresas inactivas.

Esta conexión es local y todavía no incluye login, permisos multiempresa ni pantallas Thymeleaf. No debe exponerse a una red hasta integrar autenticación. Las pruebas HTTP verifican registro, hash, relaciones, consultas, actualización, validación, duplicados y ausencia de eliminación; no acreditan ejecución contra PostgreSQL.

## Mensajes y permisos: alcance de las pruebas

El módulo nuevo agrega Mensaje, CampoMensaje, FlujoMensaje, UsoMensajeActividad y PermisoEstructura, con DTOs, servicios y repositorios JPQL. Representa la comunicación en el diagrama; no envía correos ni ejecuta procesos.

Las pruebas de servicios necesitan que los eventos puedan guardarse en la tabla compartida de nodos. El modelo anterior exige `lane_id` y `tipo_actividad` para todas las filas, aunque esos campos solo corresponden a Actividad. Por eso las pruebas aplican `src/test/resources/esquema-nodos-mensajeria.sql` únicamente a H2. No se ha corregido el modelo de Camila ni modificado PostgreSQL.

`docs/propuesta-esquema-nodos.sql` contiene el ajuste propuesto para revisión de Camila y Laura. Las pruebas verdes del módulo dependen de ese ajuste: no significan que el esquema original permita persistir eventos. Antes de integrar deben acordar también cómo mantener esa restricción al regenerar el esquema desde JPA.
