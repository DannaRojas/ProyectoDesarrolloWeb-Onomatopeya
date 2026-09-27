-- Solo para H2 en memoria. Representa el ajuste de herencia pendiente de integrar.
ALTER TABLE nodo ALTER COLUMN lane_id DROP NOT NULL;
ALTER TABLE nodo ALTER COLUMN tipo_actividad DROP NOT NULL;
ALTER TABLE nodo ADD CONSTRAINT IF NOT EXISTS ck_actividad_lane_tipo
    CHECK (tipo_nodo <> 'ACTIVIDAD' OR (lane_id IS NOT NULL AND tipo_actividad IS NOT NULL));
