# Integración por partes

Esta rama parte de `8ddb5ad`. Las clases existentes del equipo se conservan sin cambios. La configuración Maven se consolidó en el `pom.xml` principal.

## Primera conexión: Empresa

Los archivos de `integracion/empresa` reciben y validan peticiones, calculan el hash del administrador y llaman al `EmpresaService` existente. La transacción de Laura guarda la empresa y su administrador. No se expone eliminación de empresas.

Pruebas en una base H2 temporal, sin conectarse a la máquina de Laura:

```powershell
.\mvnw.cmd test
```

El `pom.xml` reúne JPA, Web MVC, Thymeleaf, Validation, PostgreSQL, Lombok, ModelMapper y las dependencias de pruebas. Security Crypto se usa solo para el hash de contraseñas; no agrega un login. H2 permanece limitado a pruebas. Se retiró `pom-integracion.xml` para evitar dos configuraciones diferentes.

ModelMapper se declara como bean en `config/ModelMapperConfig.java`, para inyectarlo en los servicios. Se fija la versión 3.2.4 de la guía oficial; la estructura bean + Service es la misma del ejemplo del curso. No se cambian las versiones de Spring Boot ni Java.

Los cinco servicios nuevos de mensajes/permisos usan ese bean. Las entradas excluyen IDs, versiones y relaciones del mapeo automático; el servicio sigue validando y resolviendo las claves foráneas. El detalle y los límites de la integración están en `docs/SEBASTIAN-MENSAJES.md`.

Para PostgreSQL, primero debe estar activo el túnel universitario y el túnel SSH. Configurar `DB_URL`, `DB_USERNAME` y `DB_PASSWORD` como variables de entorno, sin guardarlas en Git. El perfil verifica las tablas existentes con `ddl-auto=validate`; no crea ni actualiza el esquema compartido.

```powershell
.\mvnw.cmd spring-boot:run '-Dspring-boot.run.profiles=conexion-empresa'
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

Esta conexión es local y todavía no incluye login ni permisos multiempresa para Empresa. No debe exponerse a una red hasta integrar autenticación. Las pruebas HTTP verifican registro, hash, relaciones, consultas, actualización, validación, duplicados y ausencia de eliminación; no acreditan ejecución contra PostgreSQL.

## Pantallas básicas de Empresa

La vista está en `/vista/empresas`. La API JSON sigue en `/empresas`; no se cambiaron sus rutas.

- Listado y detalle de empresas.
- Registro con administrador inicial y contraseña almacenada como hash.
- Edición de nombre, NIT y correo de contacto.
- Navegación compartida con `th:fragment` y `th:replace`.
- Formularios con `th:object`, `th:field`, `@ModelAttribute`, `@Valid`, `BindingResult` y errores junto al campo.
- Sin CSS, frameworks visuales, JavaScript ni botones de eliminación.

`EmpresaVistaController` devuelve nombres de plantillas, no JSON. `EmpresaVistaService` convierte los datos con ModelMapper y delega al `EmpresaService` existente. No se modificaron las clases de Laura.

Los formularios incluyen un token de sesión para rechazar envíos sin token o desde otra sesión. Es una protección local limitada a estas vistas; no implementa autenticación, autorización ni protege la API REST anterior. Cuando se integre Spring Security, se debe sustituir por su mecanismo CSRF. Los campos editables se restringen al enlazar el formulario. Las contraseñas no se vuelven a mostrar al devolver errores.

### Vista previa sin túnel ni PostgreSQL

Solo para probar Empresa con datos ficticios en H2; se pierden al detener la aplicación:

```powershell
.\mvnw.cmd spring-boot:run '-Dspring-boot.run.useTestClasspath=true' '-Dspring-boot.run.profiles=conexion-empresa,vista-prueba' '-Dspring-boot.run.arguments=--spring.config.additional-location=file:./src/test/resources/application-vista-prueba.properties'
```

Abrir `http://127.0.0.1:8082/vista/empresas`. Este perfil vive en `src/test/resources` y no se incluye en el JAR. No es el despliegue del proyecto ni reemplaza PostgreSQL. Detener con Ctrl+C.

Para las vistas con PostgreSQL se mantiene el comando normal del perfil `conexion-empresa` y el puerto 8081. Primero deben revisar el esquema y usar datos autorizados.

Verificación local: `mvnw.cmd verify` termina con 50 pruebas aprobadas, incluidas 13 de estas vistas. También se comprobó en el navegador el registro de una empresa ficticia y la edición de su nombre, con los mensajes de confirmación correspondientes. Esta comprobación usó H2 en memoria, no PostgreSQL.

### Límites pendientes

La vista conserva el comportamiento del servicio existente: lista todas las empresas, incluidas las inactivas, sin aislamiento por usuario. El DTO actual no muestra su estado. La edición todavía no solicita una versión para detectar cambios simultáneos. No usarla como panel multiempresa real hasta completar esas reglas y la autenticación.

## Mensajes y permisos: alcance de las pruebas

El módulo nuevo agrega Mensaje, CampoMensaje, FlujoMensaje, UsoMensajeActividad y PermisoEstructura, con DTOs, servicios y repositorios JPQL. Representa la comunicación en el diagrama; no envía correos ni ejecuta procesos.

Las pruebas de servicios necesitan que los eventos puedan guardarse en la tabla compartida de nodos. El modelo anterior exige `lane_id` y `tipo_actividad` para todas las filas, aunque esos campos solo corresponden a Actividad. Por eso las pruebas aplican `src/test/resources/esquema-nodos-mensajeria.sql` únicamente a H2. No se ha corregido el modelo de Camila ni modificado PostgreSQL.

`docs/propuesta-esquema-nodos.sql` contiene el ajuste propuesto para revisión de Camila y Laura. Las pruebas verdes del módulo dependen de ese ajuste: no significan que el esquema original permita persistir eventos. Antes de integrar deben acordar también cómo mantener esa restricción al regenerar el esquema desde JPA.
