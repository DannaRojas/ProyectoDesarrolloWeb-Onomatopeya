# Parte de Sebastián: mensajes, permisos y pruebas

Rama local: `integracion-conexiones`. Base revisada: `8ddb5ad` de `origin/main`.
Fecha de revisión: 27 de septiembre de 2026.

## Qué se agregó

Se mantiene la estructura de clase: Controller → Service → Repository → base de datos.
Los DTO de entrada y salida se usan en los límites; las entidades representan las tablas.
La lógica y las transacciones están en Service. Los repositorios nuevos extienden JpaRepository y sus consultas personalizadas usan JPQL con parámetros nombrados. Mensaje incluye también una consulta nombrada con @NamedQuery.
La conversión entre DTO y entidad usa ModelMapper, inyectado en los servicios desde el bean de `config/ModelMapperConfig.java`. Las validaciones y la búsqueda de relaciones siguen en Service.

### Cómo usamos ModelMapper

- `modelMapper.map(datos, entidad)` copia los campos editables sobre la entidad existente. No reemplaza su ID, versión, estado activo ni relaciones.
- `modelMapper.map(entidad, RespuestaDto.class)` construye las respuestas de los cinco servicios de mensajes/permisos.
- La configuración declara cómo convertir relaciones como `mensaje.nodo.id` en `nodoId`.
- El servicio comprueba primero empresa, rol, versión y reglas del dominio; busca las relaciones por ID y las asigna después de validarlas.
- La conversión de entrada se usa en Mensaje, CampoMensaje, FlujoMensaje y PermisoEstructura. UsoMensajeActividad recibe únicamente una FK: su servicio busca y valida la actividad, y usa ModelMapper para la respuesta.
- Se permite copiar nulos en los campos opcionales editables para que un PUT pueda limpiar una política o una correlación. No se activa globalmente la opción de ignorar nulos.
- Los servicios anteriores de Empresa, Proceso, Pool, Lane y nodos no se modificaron.

Seguimos el patrón de [DTO y configuración del curso](https://desarrolloweb.click/contenido/backend/desarrollo/dto/) y sus [servicios](https://desarrolloweb.click/contenido/backend/desarrollo/servicios/). La versión 3.2.4 proviene de la [guía oficial de ModelMapper](https://modelmapper.org/getting-started/); se usan [mapeos explícitos y exclusiones](https://modelmapper.org/user-manual/property-mapping/) para proteger las relaciones.

| Clase | Responsabilidad | Relaciones principales |
| --- | --- | --- |
| Mensaje | Declaración de envío o recepción, nombre y correlación | Un nodo; opcionalmente un campo propio de correlación |
| CampoMensaje | Nombre, tipo y orden de los datos del mensaje | Un mensaje |
| FlujoMensaje | Conexión entre dos participantes del diagrama | Un proceso, dos pools, nodos opcionales en extremos de caja negra |
| UsoMensajeActividad | Actividades que utilizan datos recibidos | Un mensaje de recepción y una actividad de su mismo pool |
| PermisoEstructura | Regla por rol para crear, editar o retirar pools/lanes | Un proceso, rol, recurso y acción |

Cada clase tiene repositorio, servicio, DTOs y controlador.
También se agregan ContextoColaboracionService para comprobar empresa/rol y registrar historial, y ValidacionMensajesService para advertencias.
Los DELETE del módulo son retiradas lógicas: no borran filas.

## Reglas cubiertas

- Usuarios activos y empresas activas; escritura solo por administrador/editor de la empresa propietaria.
- Lectura de procesos compartidos cuando existe AccesoProceso activo. Compartir no concede edición.
- Retiradas por administrador, confirmación explícita y versión vigente.
- Campos únicos dentro de cada mensaje; el campo de correlación debe pertenecer al mismo mensaje.
- Un mensaje por nodo. Un nodo retirado o dentro de una caja negra no admite nuevas declaraciones.
- Envío desde Actividad de ENVIO o eventos compatibles; recepción desde eventos compatibles.
- Flujos entre pools diferentes del mismo proceso. Una caja negra no requiere nodo interno.
- Configuración de destino externo (correo, servicio web o cola) y tratamiento de fallo; DERIVAR exige actividad de error en el pool emisor.
- Mensajes, campos, flujos y usos se conservan al retirarse; hay registro de historial.
- Advertencias de contrato, nombre, correlación y conexiones incompletas sin impedir guardar borradores.
- Un lector nunca recibe permiso de modificación. El administrador conserva la gestión; el editor necesita permiso explícito para estructura.

## Decisiones propuestas para revisar con el equipo

Estas decisiones no sustituyen una confirmación del profesor:
- La retirada se reserva al administrador.
- Los nombres de campos retirados quedan reservados; un mensaje retirado tampoco permite crear otro sobre el mismo nodo.
- Un vínculo retirado entre mensaje y actividad sí puede reactivarse.
- Los permisos de estructura del editor empiezan deshabilitados.
- Un cambio exige enviar la versión que se consultó. Las escrituras de este módulo se serializan por proceso; los servicios anteriores todavía no participan de ese mismo control.

## Endpoints

Todos los siguientes se activan con el perfil `conexion-empresa`.
Base: `/procesos/{procesoId}`.

| Recurso | Métodos |
| --- | --- |
| /mensajes | GET, POST |
| /mensajes/{id} | PUT, DELETE |
| /mensajes/advertencias | GET |
| /mensajes/{mensajeId}/campos | GET, POST |
| /mensajes/{mensajeId}/campos/{id} | PUT, DELETE |
| /flujos-mensaje | GET, POST |
| /flujos-mensaje/{id} | PUT, DELETE |
| /mensajes/{mensajeId}/usos | GET, POST |
| /mensajes/{mensajeId}/usos/{id} | DELETE |
| /permisos-estructura | GET, PUT |
| /permisos-estructura/verificar?recurso=POOL&accion=CREAR | GET |

POST devuelve 201; GET/PUT 200; DELETE 204.
Errores: 400 datos o reglas inválidas, 401 sin sesión, 403 sin permiso, 404 registro fuera del contexto indicado/no encontrado, 409 duplicado o versión desactualizada.
Un DELETE necesita `?version=0&confirmar=true`, usando siempre la versión actual.

Ejemplo de declaración de envío, con un nodo existente del proceso:

```json
{
  "nodoId": 10,
  "sentido": "ENVIO",
  "nombre": "Solicitud",
  "origenExterno": false,
  "correlacionTipo": "NEGOCIO",
  "correlacionNegocio": "radicado"
}
```

Para correlación por campo: primero crear el mensaje sin correlación, luego crear el campo y finalmente actualizar el mensaje con `correlacionTipo: "CAMPO"`, `correlacionCampoId` y `version`.

## Cómo verificarlo ahora

Desde la raíz de esta copia:

```powershell
.\mvnw.cmd test
```

El `pom.xml` principal contiene las dependencias necesarias y configura Lombok. El POM auxiliar se retiró al consolidar la configuración.
Los tests usan H2 en memoria, no PostgreSQL de Laura.
MensajeriaServiceTest prueba persistencia, JPQL, permisos, correlación y retiradas.
MensajeriaHttpTest recorre controladores, validación, servicios y persistencia con MockMvc.
ConexionEmpresaHttpTest usa peticiones HTTP reales contra un servidor local de prueba.
Las sesiones de MensajeriaHttpTest son simuladas mediante Principal: no acreditan que ya exista un login funcional.

Verificación previa a ModelMapper, del 27 de septiembre: 28 pruebas aprobadas (5 existentes, 5 de conexión de Empresa, 10 de servicios y 8 de endpoints de mensajes). Se agregaron 9 pruebas de ModelMapper que revisan las respuestas, relaciones opcionales, conservación de IDs/versiones/estados y limpieza de campos opcionales. Las pruebas se ejecutan con Java 23 y destino de compilación Java 17. La prueba del módulo depende del ajuste de nodos en H2 descrito abajo.

Verificación posterior: `mvnw.cmd clean verify` terminó correctamente con 37 pruebas, 0 fallos, 0 errores y 0 omitidas, y generó `target/inicio-0.0.1-SNAPSHOT.jar`. Esto comprueba compilación, pruebas y empaquetado; no constituye una prueba de despliegue ni una conexión a PostgreSQL.

## Postman

Importar `postman/Onomatopeya-Mensajes.postman_collection.json`.
Configurar baseUrl e IDs reales del mismo proceso. La colección no contiene credenciales ni crea usuarios de prueba.
Los endpoints de mensajes exigen autenticación: hasta integrar el login devolverán 401 incluso con IDs válidos.
No hay una cabecera X-Usuario-Id ni un token de prueba que evite esa validación.
Cuando exista login, se debe usar su mecanismo real de autenticación en Postman. UsuarioSesionColaboracion espera el correo en Principal.getName(); si el equipo elige otro identificador, hay que ajustar esa conexión.
La colección incluye ejemplos y comprobaciones, pero no se ejecutó contra PostgreSQL ni desde Postman.

## Pendientes de integración: no dar por terminados

1. **Esquema de nodos.** La tabla SINGLE_TABLE existente obliga lane_id y tipo_actividad incluso para eventos. Las pruebas del módulo aplican un ajuste exclusivamente en H2. Revisar con Camila y Laura `docs/propuesta-esquema-nodos.sql` y acordar también las anotaciones JPA; de lo contrario, regenerar tablas recreará el problema.
2. **PostgreSQL.** Crear mediante una migración revisada las cinco tablas nuevas y sus claves foráneas. Mensaje y CampoMensaje tienen una referencia circular opcional para correlación: crear primero las tablas y después esa FK. No se ejecutó ninguna migración ni se conectó a la base compartida. La propuesta SQL de nodos no crea estas tablas.
3. **Login.** Conectar el usuario autenticado con los controladores. No exponer la integración local de Empresa en una red: sus endpoints anteriores todavía no tienen autorización.
4. **Permisos de pools/lanes.** PermisoEstructuraService.exigir debe llamarse dentro de las operaciones de escritura de esa estructura (o desde una fachada transaccional autorizada). Los servicios de Danna/Camila siguen intactos y todavía no llaman a esta comprobación. Por tanto, HU-24 no está cerrada.
5. **Publicación y edición del diagrama.** Invocar las advertencias al mostrar/publicar el proceso; coordinar retirada/cambio de nodos y pools con mensajes activos. Los servicios antiguos aún pueden cambiar esas relaciones sin pasar por las comprobaciones nuevas.
6. **Pantallas.** Se agregó una interfaz Thymeleaf básica de Empresa, documentada en `INTEGRACION.md`. Siguen pendientes las vistas de usuarios, procesos y mensajes, y el editor gráfico.
7. **Auditoría completa.** Se registran actor, proceso, acción y descripción. Las instantáneas antes/después y la visualización del historial siguen pendientes.
8. **Validación de entrega.** No marcar todas las historias como terminadas por tener estas clases. Faltan integración, pruebas en PostgreSQL y revisión de los criterios completos con el profesor.

Las clases presentes en el commit base se conservan. El `pom.xml` principal sí se actualizó para consolidar dependencias. No se cambió la copia original con trabajo local pendiente. Los commits se hicieron en esta rama; no se hizo push.
