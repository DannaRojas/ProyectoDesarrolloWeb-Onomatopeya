# Preparación de invitaciones de Usuario

Este avance es de backend: DTOs, repositorio JPQL, servicio y pruebas. No añade pantallas, endpoints, login ni envío de correos. Cubre la preparación de una invitación, no toda la HU-02.

## Cómo funciona

El punto de entrada interno es:

```java
usuarioInvitacionService.invitar(administradorId, datos);
```

`InvitarUsuarioRequestDto` recibe nombre, correo y rolAcceso. No recibe empresa, estado, contraseña, token ni ID del invitador.

El servicio:

1. Busca al administrador con su empresa mediante JPQL.
2. Exige usuario ACTIVO, rol ADMINISTRADOR y empresa activa.
3. Revisa si el correo ya existe, incluso con diferencias de mayúsculas o espacios almacenados. Las nuevas invitaciones guardan el correo en minúsculas.
4. Usa ModelMapper para los datos permitidos; el servicio establece las relaciones y el estado.
5. Guarda el usuario INVITADO, sin contraseña, asociado a la empresa del administrador y con invitadoPor.
6. Genera un token aleatorio de 32 bytes y almacena únicamente su SHA-256.
7. Devuelve el DTO seguro del usuario, el token original y su vencimiento.

El token original solo se devuelve en ese resultado inicial, para una futura integración del enlace de aceptación. No aparece en las consultas normales, no se registra en logs y no se guarda en texto plano. El resultado de invitación es sensible: no publicarlo en listados, historial, capturas o respuestas genéricas.

## Decisiones de esta primera versión

- Vigencia: 24 horas desde la creación. Es una propuesta técnica del equipo, no un plazo especificado por el profesor.
- Se permiten los tres roles definidos por el proyecto. La cuenta queda INVITADA incluso si se le asigna ADMINISTRADOR.
- Se rechaza un correo ya registrado en cualquier empresa y estado. No se traslada, reactiva ni renueva un usuario existente automáticamente.
- Repetir la invitación no cambia el token previo, aunque haya vencido. Renovación y aceptación se implementarán por separado.
- La comprobación por correo es global; no se revela en el error a qué empresa pertenece. La restricción única existente sigue actuando como respaldo ante duplicados exactos concurrentes.
- La normalización aplica a este servicio nuevo. No corrige correos existentes ni modifica el registro de Empresa. La unicidad sin distinguir mayúsculas entre todos los caminos de escritura requiere revisar la normalización y el índice de base de datos con Laura; el índice actual no garantiza por sí solo esa regla.

## Qué prueban los tests

`UsuarioInvitacionServiceTest` usa una base H2 temporal y comprueba:

- Empresa, invitador, rol, estado, contraseña ausente, fecha y versión.
- Token aleatorio, hash almacenado y vencimiento.
- Asignación de los tres roles sin activar la cuenta.
- Rechazo de editor, lector, administrador invitado/inactivo y empresa inactiva.
- IDs inválidos y actor inexistente.
- Correo duplicado de otra empresa sin traslado ni cambios.
- Invitación repetida sin reemplazar el token.
- Validaciones de campos y longitudes.
- Tokens distintos y DTO sin campos para escoger otra empresa.

Para ejecutar:

```powershell
.\mvnw.cmd '-Dtest=UsuarioInvitacionServiceTest' test
.\mvnw.cmd verify
```

## Qué falta y qué no debemos afirmar

Verificación del 27/09/2026: `mvnw.cmd -o -q verify` terminó correctamente con 66 pruebas, cero fallos, cero errores y cero omitidas. Nueve corresponden a invitaciones. También se generó el JAR. El vencimiento se guarda con precisión de microsegundos para coincidir con el valor recuperado de la base.

No se envió correo. No existe todavía aceptación de invitación, consumo del token, establecimiento de contraseña, renovación, cambio de rol o desactivación. Tampoco se ha probado este servicio contra PostgreSQL.

`administradorId` es un dato interno confiable en estas pruebas. No hay autenticación real: un futuro controlador deberá obtenerlo de la sesión autenticada, nunca aceptar el ID que alguien envíe como prueba de identidad. Verificar el rol de ese ID no autentica al llamador.

La HU-02 exige invitación por correo y gestión de usuarios; este avance por sí solo no la completa. La entrega 1 se centra en backend según la [rúbrica actual](https://desarrolloweb.click/calificacion/#primera-entrega-aplicacion-backend-con-spring-boot-y-jpa); no añadimos interfaz ni adelantamos Spring Security.

## Archivos y conservación

Se agregaron InvitarUsuarioRequestDto, InvitacionUsuarioResponseDto, UsuarioInvitacionService y su prueba. Se ampliaron UsuarioConsultaRepository y ModelMapperConfig de nuestra integración.

Usuario.java, UsuarioRepository.java y EmpresaService.java de Laura no se modificaron. No se cambió el esquema, no se aplicó la migración ni se escribieron datos en su PostgreSQL.
