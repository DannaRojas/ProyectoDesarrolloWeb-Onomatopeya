# Propuesta para sincronizar PostgreSQL

Estado: pendiente de revisión y de prueba en una copia de PostgreSQL. No aplicada.
Fecha de la revisión de la base: 27 de septiembre de 2026.
Rama: `integracion-conexiones`. Código de referencia: `b99c8ab`.

## Qué encontramos

La conexión SSH y las credenciales de la aplicación funcionan. PostgreSQL 14.24 está activo en la máquina de Laura y la base es `gestion_procesos`.

La consulta de metadatos encontró ocho tablas: empresa, usuario, proceso, rol_proceso, pool, lane, acceso_proceso e historial_cambio. Había tres empresas, cero usuarios y cero procesos. Las tres empresas tenían correo y versión.

El arranque del proyecto con `ddl-auto=validate` sí abrió una conexión PostgreSQL, pero se detuvo en `Schema validation: missing table [arco]`. Ese es el primer error detectado, no el único ajuste pendiente. No se crearon tablas ni se cambiaron registros durante la comprobación.

## Qué propone el archivo SQL

Archivo: [propuesta-integracion-postgresql.sql](propuesta-integracion-postgresql.sql).

| Parte | Propuesta | Motivo |
| --- | --- | --- |
| Empresa | Ampliar NIT de 30 a 255 y correo de 254 a 255 caracteres; exigir correo no nulo | Coincidir con el modelo Java y los formularios actuales sin modificar las clases de Laura |
| Nodo | Crear una tabla para Actividad, Evento y Gateway | Las clases usan herencia SINGLE_TABLE, no tres tablas separadas |
| Arco | Crear las relaciones entre nodos | Hace referencia a proceso, pool, origen y destino |
| Mensaje y CampoMensaje | Crear ambas tablas y añadir después la FK de correlación | Cada campo pertenece a un mensaje; el mensaje puede escoger un campo para correlacionar |
| FlujoMensaje | Crear las conexiones de comunicación | Relaciona pools, nodos y una actividad de error opcional |
| UsoMensajeActividad | Crear la tabla de asociación | Un mensaje puede ser usado por actividades |
| PermisoEstructura | Crear permisos por proceso, rol, recurso y acción | Evita repetir la misma combinación |

Serían siete tablas nuevas y quince tablas de dominio en total, si el esquema no cambia desde la revisión. Las clases hijas de Nodo no cuentan como tablas adicionales.

Las nuevas tablas incluyen identidades autogeneradas, claves foráneas, restricciones únicas, valores admitidos de los enums y los campos de versión donde existen en Java. Se agregan índices de las relaciones no cubiertas por el inicio de una clave única.

No se incluyen usuarios, contraseñas, empresas de ejemplo, permisos iniciales ni datos de demostración. Los roles de acceso son enums: este modelo no necesita una tabla adicional para ADMINISTRADOR, EDITOR y LECTURA.

## Qué necesitamos acordar

### Con Laura

- Aprobar que ampliemos las longitudes de Empresa para respetar el código actual. Otra opción es conservar los límites de la base y ajustar DTOs/formularios y entidades, pero eso requiere un cambio de código separado y acordado.
- Confirmar que el correo de contacto sea obligatorio. Si aparecen correos nulos antes de aplicar la migración, el script se detiene: no inventa ni rellena valores.
- Revisar un respaldo y cómo restaurarlo antes de cualquier aplicación real.
- Volver a consultar el esquema antes de ejecutar: pudo cambiar después del 27 de septiembre.
- Verificar quién debe ser propietario de los objetos y con qué usuario se aplicará el SQL. El archivo no crea roles, cambia propietarios ni concede permisos.

### Con Camila y Laura

En `Actividad`, `lane_id` y `tipo_actividad` están marcados como obligatorios. En la tabla compartida esos campos no corresponden a Evento ni Gateway.

El SQL permite nulos en esas dos columnas, pero agrega `ck_actividad_lane_tipo`: cuando `tipo_nodo = 'ACTIVIDAD'`, ambos siguen siendo obligatorios.

No se modificaron las anotaciones de Camila. Debemos acordar cómo reflejar esta regla en JPA y comprobar inserciones reales de los tres subtipos. Si se regenera el esquema desde las anotaciones actuales, puede reaparecer el problema. Mantener `ddl-auto=validate`, no cambiarlo a `update` o `create` para salvar el arranque.

El archivo anterior `docs/propuesta-esquema-nodos.sql` sirve para una tabla Nodo ya existente; **no hay que ejecutarlo junto con esta propuesta**, que crea Nodo con el ajuste incorporado.

## Qué sigue siendo responsabilidad del código

Una FK comprueba que un registro existe, pero no todas las reglas del proyecto. Entre otras:

- Que nodo, pool, lane y arco correspondan al mismo proceso.
- Que la lane corresponda al rol y al pool adecuados.
- Que una referencia a Actividad apunte realmente al subtipo ACTIVIDAD de Nodo.
- Que el campo de correlación pertenezca al mismo mensaje.
- Que exista autorización para leer o modificar el proceso.
- Que un proceso no cambie de empresa y que no se borren empresas.

La propuesta no introduce borrados en cascada, pero eso no equivale a impedir cualquier DELETE directo por un usuario de base de datos con permisos. No agrega triggers ni cambia permisos. Tampoco completa el login, la auditoría o las historias pendientes.

## Cómo revisar y probar sin tocar la base compartida

1. Laura prepara un respaldo y una copia aislada de la base. La copia debe llamarse `gestion_procesos` en otra instancia o debe revisarse explícitamente la comprobación del nombre en el SQL. No apuntar el ensayo al servidor compartido.
2. Comparar columnas, restricciones y datos existentes con la revisión de arriba.
3. Abrir el SQL y comprobar el destino de conexión. Se ejecuta con psql por contener `\set ON_ERROR_STOP on`.
4. Ensayarlo en esa copia. Empieza una transacción, limita los tiempos de bloqueo y termina en **ROLLBACK**. Esto revierte los cambios del ensayo; aun así, toma bloqueos mientras se ejecuta.
5. Si una tabla nueva ya existe, falta una tabla previa o hay un correo nulo, se detiene sin sobrescribir ni corregir datos. Investigar el error; no saltarse la comprobación con `IF NOT EXISTS`.
6. Tras revisar el ensayo, cambiar únicamente el ROLLBACK final por COMMIT en una copia aprobada del archivo y aplicarlo **solo en la base aislada**. Hibernate, desde otra conexión, no puede validar tablas aún no confirmadas.
7. Arrancar el proyecto apuntando a esa copia con `ddl-auto=validate`. Corregir cualquier diferencia restante antes de plantear la aplicación al servidor de Laura.
8. Probar registro de Empresa con administrador, consulta y edición; NIT/correo repetidos; creación de Actividad, Evento y Gateway; relaciones de mensajes y permisos. Usar datos ficticios en la copia.
9. Revisar conservación de registros existentes y restricciones. Solo después, con autorización del equipo y respaldo verificado, planear la aplicación real.

Una segunda ejecución confirmada debe detenerse porque las tablas ya existen. El script no es idempotente a propósito: no oculta diferencias entre el esquema esperado y el encontrado.

Si psql se detiene por un error en una sesión interactiva, ejecutar ROLLBACK antes de seguir. Después de un COMMIT real, no deshacer con DROP TABLE ni reducir columnas sin revisar los datos nuevos; preparar una corrección o restauración coordinada.

## Verificación realizada y pendiente

Se contrastaron las siete definiciones de tabla con las entidades y enums actuales, el orden de sus referencias y las restricciones únicas. Se revisó que no hubiera DROP TABLE, TRUNCATE, DELETE ni COMMIT ejecutable, y que el archivo quedara fuera de los recursos de arranque.

**No se ejecutó este SQL en PostgreSQL ni se certifica todavía su funcionamiento allí.** Las 50 pruebas previas del proyecto usan H2 y no validan esta migración. Falta el ensayo y la validación de Hibernate en una copia PostgreSQL.

No se modificó código de Laura, Camila o Danna. Esta propuesta no autoriza por sí misma cambiar la base compartida.
