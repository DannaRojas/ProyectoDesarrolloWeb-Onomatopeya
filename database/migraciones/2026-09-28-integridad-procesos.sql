-- Refuerza las cuatro tablas existentes sin cambiar datos ni relaciones.
-- Ejecutar con psql -v ON_ERROR_STOP=1. Si falla algo, no confirmar la transacción.
BEGIN;
SET LOCAL lock_timeout = '5s';
SET LOCAL statement_timeout = '30s';
SET LOCAL search_path = public;

DO $$
BEGIN
    IF current_database() <> 'gestion_procesos' THEN
        RAISE EXCEPTION 'Esta migración corresponde a gestion_procesos';
    END IF;
END $$;

-- JPA necesita una versión no nula para controlar las ediciones simultáneas.
-- No se rellenan versiones: si hubiera valores nulos, la migración se detiene.
ALTER TABLE proceso ALTER COLUMN version SET NOT NULL;
ALTER TABLE rol_proceso ALTER COLUMN version SET NOT NULL;
ALTER TABLE pool ALTER COLUMN version SET NOT NULL;
ALTER TABLE lane ALTER COLUMN version SET NOT NULL;

-- Los valores deben coincidir con EstadoPublicacion y TipoParticipante.
ALTER TABLE proceso ADD CONSTRAINT ck_proceso_estado_publicacion
    CHECK (estado_publicacion IN ('BORRADOR', 'PUBLICADO'));
ALTER TABLE pool ADD CONSTRAINT ck_pool_tipo_participante
    CHECK (tipo_participante IN ('EMPRESA', 'CLIENTE', 'PROVEEDOR', 'SISTEMA_EXTERNO'));

COMMIT;
