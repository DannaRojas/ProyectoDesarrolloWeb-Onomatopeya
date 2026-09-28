-- Prueba las restricciones en copias temporales; no inserta datos reales.
-- LIKE no copia las llaves foráneas: esta prueba no certifica esas relaciones.
BEGIN;
SET LOCAL statement_timeout = '30s';
CREATE TEMP TABLE prueba_proceso (LIKE public.proceso INCLUDING ALL) ON COMMIT DROP;
CREATE TEMP TABLE prueba_rol_proceso (LIKE public.rol_proceso INCLUDING ALL) ON COMMIT DROP;
CREATE TEMP TABLE prueba_pool (LIKE public.pool INCLUDING ALL) ON COMMIT DROP;
CREATE TEMP TABLE prueba_lane (LIKE public.lane INCLUDING ALL) ON COMMIT DROP;

-- Los IDs explícitos evitan consumir las secuencias compartidas.
INSERT INTO prueba_proceso (id, empresa_id, creador_id, nombre)
VALUES (1, 1, 1, 'Prueba temporal');
INSERT INTO prueba_rol_proceso (id, empresa_id, nombre)
VALUES (1, 1, 'Rol temporal');
INSERT INTO prueba_pool
    (id, proceso_id, nombre, tipo_participante, posicion_x, posicion_y, ancho, alto, orden)
VALUES (1, 1, 'Pool temporal', 'EMPRESA', 0, 0, 1000, 600, 1);
INSERT INTO prueba_lane (id, pool_id, rol_proceso_id, orden, altura)
VALUES (1, 1, 1, 1, 200);

DO $$
DECLARE
    tabla text;
    valor text;
BEGIN
    FOREACH valor IN ARRAY ARRAY['BORRADOR', 'PUBLICADO'] LOOP
        UPDATE prueba_proceso SET estado_publicacion = valor;
    END LOOP;
    FOREACH valor IN ARRAY ARRAY['EMPRESA', 'CLIENTE', 'PROVEEDOR', 'SISTEMA_EXTERNO'] LOOP
        UPDATE prueba_pool SET tipo_participante = valor;
    END LOOP;
    BEGIN
        UPDATE prueba_proceso SET estado_publicacion = 'INVALIDO';
        RAISE EXCEPTION 'No se rechazó un estado de publicación inválido';
    EXCEPTION WHEN check_violation THEN
        RAISE NOTICE 'OK: estado inválido rechazado';
    END;
    BEGIN
        UPDATE prueba_pool SET tipo_participante = 'INVALIDO';
        RAISE EXCEPTION 'No se rechazó un tipo de participante inválido';
    EXCEPTION WHEN check_violation THEN
        RAISE NOTICE 'OK: participante inválido rechazado';
    END;
    FOREACH tabla IN ARRAY ARRAY['prueba_proceso', 'prueba_rol_proceso', 'prueba_pool', 'prueba_lane'] LOOP
        BEGIN
            EXECUTE format('UPDATE pg_temp.%I SET version = NULL', tabla);
            RAISE EXCEPTION 'Se permitió una versión nula en %', tabla;
        EXCEPTION WHEN not_null_violation THEN
            RAISE NOTICE 'OK: versión nula rechazada en %', tabla;
        END;
        EXECUTE format('UPDATE pg_temp.%I SET version = version + 1', tabla);
    END LOOP;
    RAISE NOTICE 'OK: valores válidos y aumentos de versión aceptados';
END $$;
ROLLBACK;
