ALTER TABLE solicitudes
  ADD COLUMN condiciones_snapshot_id VARCHAR(100) NULL AFTER condiciones_cancelacion_aceptadas,
  ADD COLUMN condiciones_snapshot_hash CHAR(64) NULL AFTER condiciones_snapshot_id,
  ADD COLUMN deduplication_key BINARY(32) NULL AFTER consentimiento_verificado,
  ADD COLUMN aggregate_version BIGINT NOT NULL DEFAULT 1 AFTER deduplication_key,
  ADD UNIQUE KEY uq_solicitudes_pendiente_dedup (deduplication_key),
  ADD CONSTRAINT ck_solicitudes_consentimiento_verificable CHECK (
    consentimiento_verificado = FALSE OR (
      condiciones_snapshot_id IS NOT NULL AND
      condiciones_snapshot_hash IS NOT NULL AND
      condiciones_aceptadas_en IS NOT NULL
    )
  );

ALTER TABLE reservas
  ADD COLUMN ocupacion_desde DATETIME(6) NULL AFTER hasta,
  ADD COLUMN ocupacion_hasta DATETIME(6) NULL AFTER ocupacion_desde,
  ADD COLUMN periodo_liberado_desde DATETIME(6) NULL AFTER ocupacion_hasta,
  ADD COLUMN condiciones_snapshot_id VARCHAR(100) NULL AFTER condiciones_cancelacion_acordadas,
  ADD COLUMN condiciones_snapshot_hash CHAR(64) NULL AFTER condiciones_snapshot_id,
  ADD COLUMN condiciones_aceptadas_en DATETIME(6) NULL AFTER condiciones_snapshot_hash;

UPDATE reservas
SET ocupacion_desde = desde,
    ocupacion_hasta = hasta;

UPDATE reservas r
JOIN solicitudes s ON s.id = r.solicitud_id
SET r.condiciones_snapshot_id = s.condiciones_snapshot_id,
    r.condiciones_snapshot_hash = s.condiciones_snapshot_hash,
    r.condiciones_aceptadas_en = CASE
      WHEN s.consentimiento_verificado = TRUE THEN s.condiciones_aceptadas_en
      ELSE NULL
    END;

ALTER TABLE reservas
  MODIFY COLUMN ocupacion_desde DATETIME(6) NOT NULL,
  MODIFY COLUMN ocupacion_hasta DATETIME(6) NOT NULL,
  ADD INDEX idx_reservas_agenda_ocupacion
    (agenda_id, estado, ocupacion_desde, ocupacion_hasta),
  ADD CONSTRAINT ck_reservas_ocupacion CHECK (
    ocupacion_desde = desde AND
    ocupacion_hasta > ocupacion_desde AND
    ocupacion_hasta <= hasta AND
    (periodo_liberado_desde IS NULL OR periodo_liberado_desde = ocupacion_hasta)
  );

UPDATE outbox_events
SET aggregate_type = 'SOLICITUD'
WHERE event_type LIKE 'Solicitud%';

ALTER TABLE outbox_events
  ADD COLUMN claimed_by VARCHAR(36) NULL AFTER last_error,
  ADD COLUMN claimed_at DATETIME(6) NULL AFTER claimed_by,
  ADD COLUMN next_attempt_at DATETIME(6) NULL AFTER claimed_at,
  ADD UNIQUE KEY uq_outbox_aggregate_version
    (aggregate_type, aggregate_id, aggregate_version),
  ADD INDEX idx_outbox_claimable
    (published_at, attempts, next_attempt_at, claimed_at, occurred_at);
