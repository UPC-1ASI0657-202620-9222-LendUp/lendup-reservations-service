ALTER TABLE solicitudes
  MODIFY COLUMN condiciones_aceptadas_en DATETIME(6) NULL,
  ADD COLUMN consentimiento_verificado BOOLEAN NOT NULL DEFAULT FALSE AFTER condiciones_aceptadas_en;

ALTER TABLE reservas
  ADD COLUMN aggregate_version BIGINT NOT NULL DEFAULT 1 AFTER external_event_version;

