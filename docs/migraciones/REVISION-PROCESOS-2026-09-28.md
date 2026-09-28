# Revisión de Proceso, RolProceso, Pool y Lane

Fecha: 28 de septiembre de 2026.
Destino: PostgreSQL de Laura, base gestion_procesos, esquema public.
Estado: ajustes aplicados y comprobados.

## Lo que ya estaba bien

Las cuatro tablas coincidían con el SQL compartido por el equipo y con los
tipos y relaciones de las entidades actuales. Se conservaron las claves
primarias, foráneas, restricciones únicas, secuencias y valores predeterminados.
El índice parcial del pool propietario ya existía y se conservó.
Las cuatro tablas tenían cero registros antes y después de la operación.

## Cambios aplicados

- version ahora es NOT NULL en las cuatro tablas; mantiene DEFAULT 0.
  Evita guardar versiones nulas que no sirven para el control de concurrencia de JPA.
- proceso.estado_publicacion admite solamente BORRADOR o PUBLICADO.
- pool.tipo_participante admite EMPRESA, CLIENTE, PROVEEDOR o SISTEMA_EXTERNO.
  Ambos conjuntos se tomaron de los enums existentes, sin inventar estados.

SQL: 2026-09-28-integridad-procesos.sql.
Es una migración de una sola aplicación: no repetirla, porque las restricciones
ya existen. No se ejecuta automáticamente al iniciar Spring Boot.
No se modificó ninguna clase Java ni ningún registro existente.
No se aplicó propuesta-integracion-postgresql.sql.

## Comprobación realizada

Primero se ensayó la migración y la prueba en una misma transacción que terminó
en ROLLBACK. Se confirmó después que version seguía admitiendo nulos.
Luego se repitió el ajuste con las pruebas dentro de la transacción y se confirmó
con COMMIT únicamente al pasar todas las comprobaciones.

La prueba está en prueba-integridad-procesos.sql y utiliza copias temporales:

- Acepta los dos estados de publicación y los cuatro tipos de participante.
- Rechaza un estado y un tipo inexistentes.
- Rechaza version NULL en cada una de las cuatro tablas.
- Acepta valores predeterminados y aumentos de versión.

Los IDs de prueba fueron explícitos: no se consumieron secuencias reales.
Las tablas temporales desaparecieron al cerrar la transacción.
Se comprobaron después del COMMIT los cuatro NOT NULL, los dos CHECK validados,
el índice parcial y los conteos originales.

Las copias temporales no incluyen las FK. Esta prueba no comprueba las relaciones
mediante inserciones, el bloqueo optimista de Hibernate ni el funcionamiento de
la aplicación completa.

## Reglas que siguen siendo responsabilidad de Service

- ProcesoService comprueba que el creador pertenezca a la empresa al crear el
  proceso y crea su pool propietario en la misma transacción.
- LaneService comprueba que el rol y el proceso del pool pertenezcan a la misma empresa.
- PoolService impide retirar el pool propietario.
- El índice garantiza como máximo un propietario activo, no la existencia de uno.
- Las FK no impiden por sí mismas cambiar la empresa de un proceso ni borrar una
  empresa sin referencias. No se añadieron triggers ni se cambiaron permisos.

No debe añadirse una FK que obligue a mantener siempre la misma empresa del
creador y del proceso: el usuario puede cambiar de empresa, pero el proceso no.

## Alcance pendiente

Actualización posterior: las siete tablas pendientes ya se crearon y Hibernate
validó el esquema. Ver CREACION-DIAGRAMA-2026-09-28.md. El párrafo siguiente
describe el estado antes de esa creación.

La base sigue teniendo ocho tablas. Faltan las siete tablas documentadas en
REVISION-POSTGRESQL.md para integrar todo el modelo. Estos ajustes no resuelven
el arranque completo contra PostgreSQL ni certifican toda la entrega.

## Reversión, solo si se acuerda con el equipo

Los cambios son de restricciones, no de datos. Para regresar al esquema anterior,
se pueden retirar únicamente ck_proceso_estado_publicacion y
ck_pool_tipo_participante y quitar NOT NULL de version en estas cuatro tablas,
dentro de una transacción con tiempo de bloqueo limitado. Esto debilita las
validaciones y requiere una decisión explícita; no se ejecutó ninguna reversión
después de confirmar la migración.
