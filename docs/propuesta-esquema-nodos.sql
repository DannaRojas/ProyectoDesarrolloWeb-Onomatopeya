-- PROPUESTA PARA REVISAR CON CAMILA Y LAURA. No se ejecuta al iniciar la aplicación.
-- En SINGLE_TABLE los campos de Actividad son opcionales para Evento y Gateway.
-- La condición mantiene lane y tipo obligatorios únicamente para actividades.
-- Ejecutar solo tras revisar respaldo, esquema destino y anotaciones JPA.
BEGIN;
ALTER TABLE nodo ALTER COLUMN lane_id DROP NOT NULL;
ALTER TABLE nodo ALTER COLUMN tipo_actividad DROP NOT NULL;
ALTER TABLE nodo ADD CONSTRAINT ck_actividad_lane_tipo
    CHECK (tipo_nodo <> 'ACTIVIDAD' OR (lane_id IS NOT NULL AND tipo_actividad IS NOT NULL));
COMMIT;
