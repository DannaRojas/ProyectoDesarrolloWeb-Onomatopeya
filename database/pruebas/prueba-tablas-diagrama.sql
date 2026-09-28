-- Ejecutar con psql -v ON_ERROR_STOP=1. No deja datos ni consume secuencias.
-- Los IDs de prueba son explícitos y negativos; cualquier colisión detiene la prueba.
BEGIN;
SET LOCAL lock_timeout = '5s';
SET LOCAL statement_timeout = '30s';
SET LOCAL search_path = public, pg_catalog;

INSERT INTO empresa (id,nombre,nit,correo_contacto) OVERRIDING SYSTEM VALUE
VALUES (-928001,'Ensayo de integración','ENSAYO-928001','ensayo-928001@example.invalid');
INSERT INTO usuario (id,empresa_id,nombre,correo,rol_acceso,estado)
VALUES (-928001,-928001,'Ensayo','ensayo-928001@example.invalid','ADMINISTRADOR','ACTIVO');
INSERT INTO proceso (id,empresa_id,creador_id,nombre)
VALUES (-928001,-928001,-928001,'Ensayo');
INSERT INTO rol_proceso (id,empresa_id,nombre) VALUES (-928001,-928001,'Ensayo');
INSERT INTO pool (id,proceso_id,nombre,tipo_participante,propietario,posicion_x,posicion_y,ancho,alto,orden)
VALUES (-928001,-928001,'Propietario','EMPRESA',true,0,0,1000,600,1),
       (-928002,-928001,'Externo','CLIENTE',false,0,700,1000,600,2);
INSERT INTO lane (id,pool_id,rol_proceso_id,orden,altura)
VALUES (-928001,-928001,-928001,1,300);
INSERT INTO nodo (id,tipo_nodo,proceso_id,pool_id,nombre,posicion_x,posicion_y,ancho,alto,estado,tipo_actividad,lane_id)
VALUES (-928001,'ACTIVIDAD',-928001,-928001,'Enviar',10,10,100,60,'BORRADOR','ENVIO',-928001);
INSERT INTO nodo (id,tipo_nodo,proceso_id,pool_id,nombre,posicion_x,posicion_y,ancho,alto,estado,tipo_evento,naturaleza_evento,operacion_mensaje)
VALUES (-928002,'EVENTO',-928001,-928002,'Recibir',10,10,50,50,'BORRADOR','INICIO','MENSAJE','RECEPCION');
INSERT INTO nodo (id,tipo_nodo,proceso_id,pool_id,nombre,posicion_x,posicion_y,ancho,alto,estado,tipo_gateway,direccion_gateway)
VALUES (-928003,'GATEWAY',-928001,-928001,'Decidir',120,10,50,50,'BORRADOR','EXCLUSIVO','DIVERGENCIA');
INSERT INTO arco (id,proceso_id,pool_id,origen_id,destino_id)
VALUES (-928001,-928001,-928001,-928001,-928003);
INSERT INTO mensaje (id,nodo_id,sentido,nombre)
VALUES (-928001,-928001,'ENVIO','Solicitud');
INSERT INTO campo_mensaje (id,mensaje_id,nombre,tipo_dato,orden)
VALUES (-928001,-928001,'referencia','TEXTO',1);
UPDATE mensaje SET correlacion_tipo='CAMPO',correlacion_campo_id=-928001
WHERE id=-928001;
INSERT INTO flujo_mensaje (id,proceso_id,pool_origen_id,pool_destino_id,nodo_origen_id,nodo_destino_id,politica_fallo,actividad_error_id)
VALUES (-928001,-928001,-928001,-928002,-928001,-928002,'DERIVAR',-928001);
INSERT INTO uso_mensaje_actividad (id,mensaje_id,actividad_id)
VALUES (-928001,-928001,-928001);
INSERT INTO permiso_estructura (id,proceso_id,rol_acceso,recurso,accion,permitido)
VALUES (-928001,-928001,'EDITOR','POOL','CREAR',true);

DO $pruebas$
DECLARE
    caso record;
    estado_error text;
    restricciones integer := 0;
BEGIN
    -- Verifica cada FK de las siete tablas contra un identificador inexistente.
    FOR caso IN
        SELECT c.relname tabla,a.attname columna
        FROM pg_constraint k
        JOIN pg_class c ON c.oid=k.conrelid
        JOIN pg_attribute a ON a.attrelid=c.oid AND a.attnum=k.conkey[1]
        WHERE k.contype='f' AND c.relnamespace=current_schema()::regnamespace
          AND c.relname IN ('nodo','arco','mensaje','campo_mensaje','flujo_mensaje','uso_mensaje_actividad','permiso_estructura')
    LOOP
        BEGIN
            EXECUTE format('UPDATE %I SET %I=-9223372036854770000 WHERE id=-928001',caso.tabla,caso.columna);
            RAISE EXCEPTION 'La FK %.% aceptó un ID inexistente',caso.tabla,caso.columna;
        EXCEPTION WHEN foreign_key_violation THEN
            restricciones := restricciones + 1;
        END;
    END LOOP;
    IF restricciones <> 19 THEN
        RAISE EXCEPTION 'Se esperaban 19 FK verificadas, se probaron %',restricciones;
    END IF;
    RAISE NOTICE 'OK: % llaves foráneas rechazan referencias inexistentes',restricciones;

    FOR caso IN SELECT * FROM (VALUES
        ('UPDATE nodo SET tipo_nodo=''OTRO'' WHERE id=-928001','23514'),
        ('UPDATE nodo SET estado=''OTRO'' WHERE id=-928001','23514'),
        ('UPDATE nodo SET tipo_actividad=''OTRO'' WHERE id=-928001','23514'),
        ('UPDATE nodo SET tipo_actividad=NULL WHERE id=-928001','23514'),
        ('UPDATE nodo SET lane_id=NULL WHERE id=-928001','23514'),
        ('UPDATE nodo SET tipo_gateway=''OTRO'' WHERE id=-928003','23514'),
        ('UPDATE nodo SET direccion_gateway=''OTRO'' WHERE id=-928003','23514'),
        ('UPDATE nodo SET tipo_evento=''OTRO'' WHERE id=-928002','23514'),
        ('UPDATE nodo SET naturaleza_evento=''OTRO'' WHERE id=-928002','23514'),
        ('UPDATE nodo SET operacion_mensaje=''OTRO'' WHERE id=-928002','23514'),
        ('UPDATE mensaje SET sentido=''OTRO'' WHERE id=-928001','23514'),
        ('UPDATE mensaje SET correlacion_tipo=''OTRO'' WHERE id=-928001','23514'),
        ('UPDATE mensaje SET politica_sin_correspondencia=''OTRO'' WHERE id=-928001','23514'),
        ('UPDATE campo_mensaje SET tipo_dato=''OTRO'' WHERE id=-928001','23514'),
        ('UPDATE flujo_mensaje SET tipo_destino=''OTRO'' WHERE id=-928001','23514'),
        ('UPDATE flujo_mensaje SET politica_fallo=''OTRO'' WHERE id=-928001','23514'),
        ('UPDATE permiso_estructura SET rol_acceso=''OTRO'' WHERE id=-928001','23514'),
        ('UPDATE permiso_estructura SET recurso=''OTRO'' WHERE id=-928001','23514'),
        ('UPDATE permiso_estructura SET accion=''OTRO'' WHERE id=-928001','23514'),
        ('INSERT INTO arco (id,proceso_id,pool_id,origen_id,destino_id) VALUES (-928099,-928001,-928001,-928001,-928003)','23505'),
        ('INSERT INTO mensaje (id,nodo_id,sentido,nombre) VALUES (-928099,-928001,''ENVIO'',''Duplicado'')','23505'),
        ('INSERT INTO campo_mensaje (id,mensaje_id,nombre,tipo_dato,orden) VALUES (-928099,-928001,''referencia'',''TEXTO'',2)','23505'),
        ('INSERT INTO uso_mensaje_actividad (id,mensaje_id,actividad_id) VALUES (-928099,-928001,-928001)','23505'),
        ('INSERT INTO permiso_estructura (id,proceso_id,rol_acceso,recurso,accion,permitido) VALUES (-928099,-928001,''EDITOR'',''POOL'',''CREAR'',false)','23505'),
        ('UPDATE mensaje SET version=NULL WHERE id=-928001','23502'),
        ('UPDATE campo_mensaje SET version=NULL WHERE id=-928001','23502'),
        ('UPDATE flujo_mensaje SET version=NULL WHERE id=-928001','23502'),
        ('UPDATE uso_mensaje_actividad SET version=NULL WHERE id=-928001','23502'),
        ('UPDATE permiso_estructura SET version=NULL WHERE id=-928001','23502')
    ) AS casos(sentencia,esperado)
    LOOP
        BEGIN
            EXECUTE caso.sentencia;
            RAISE EXCEPTION 'La restricción no se aplicó: %',caso.sentencia;
        EXCEPTION WHEN OTHERS THEN
            GET STACKED DIAGNOSTICS estado_error=RETURNED_SQLSTATE;
            IF estado_error <> caso.esperado THEN
                RAISE EXCEPTION 'Error inesperado % (esperado %) en %',estado_error,caso.esperado,caso.sentencia;
            END IF;
        END;
    END LOOP;
    RAISE NOTICE 'OK: 29 casos de CHECK, obligatoriedad y unicidad';
    RAISE NOTICE 'OK: Actividad, Evento, Gateway y las siete tablas admiten datos relacionados válidos';
END;
$pruebas$;
ROLLBACK;
