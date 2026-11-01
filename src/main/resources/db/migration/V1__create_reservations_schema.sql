CREATE TABLE solicitudes (
  id BINARY(16) NOT NULL,
  publicacion_id BINARY(16) NOT NULL,
  prestatario_usuario_id BINARY(16) NOT NULL,
  prestamista_usuario_id BINARY(16) NOT NULL,
  desde DATETIME(6) NOT NULL,
  hasta DATETIME(6) NOT NULL,
  estado VARCHAR(20) NOT NULL DEFAULT 'PENDIENTE',
  tarifa_diaria_aceptada DECIMAL(12,2) NOT NULL,
  garantia_monetaria_aceptada DECIMAL(12,2) NULL,
  moneda_aceptada CHAR(3) NOT NULL,
  lugar_intercambio_aceptado VARCHAR(250) NOT NULL,
  condiciones_uso_aceptadas TEXT NOT NULL,
  condiciones_entrega_aceptadas TEXT NOT NULL,
  condiciones_devolucion_aceptadas TEXT NOT NULL,
  condiciones_cancelacion_aceptadas TEXT NOT NULL,
  condiciones_aceptadas_en DATETIME(6) NOT NULL,
  creada_en DATETIME(6) NOT NULL,
  respondida_en DATETIME(6) NULL,
  motivo_rechazo TEXT NULL,
  cancelada_en DATETIME(6) NULL,
  motivo_cancelacion TEXT NULL,
  actualizada_en DATETIME(6) NOT NULL,
  PRIMARY KEY (id),
  CONSTRAINT ck_solicitudes_periodo CHECK (desde < hasta),
  CONSTRAINT ck_solicitudes_tarifa CHECK (tarifa_diaria_aceptada > 0),
  CONSTRAINT ck_solicitudes_garantia CHECK (garantia_monetaria_aceptada IS NULL OR garantia_monetaria_aceptada >= 0),
  INDEX idx_solicitudes_prestatario (prestatario_usuario_id, estado, creada_en),
  INDEX idx_solicitudes_prestamista (prestamista_usuario_id, estado, creada_en),
  INDEX idx_solicitudes_publicacion_periodo (publicacion_id, desde, hasta)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE agendas_objeto (
  id BINARY(16) NOT NULL,
  publicacion_id BINARY(16) NOT NULL,
  version BIGINT NOT NULL DEFAULT 0,
  creada_en DATETIME(6) NOT NULL,
  actualizada_en DATETIME(6) NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uq_agendas_publicacion (publicacion_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE reservas (
  id BINARY(16) NOT NULL,
  solicitud_id BINARY(16) NOT NULL,
  agenda_id BINARY(16) NOT NULL,
  desde DATETIME(6) NOT NULL,
  hasta DATETIME(6) NOT NULL,
  estado VARCHAR(32) NOT NULL DEFAULT 'CONFIRMADA',
  tarifa_diaria_acordada DECIMAL(12,2) NOT NULL,
  garantia_monetaria_acordada DECIMAL(12,2) NULL,
  moneda_acordada CHAR(3) NOT NULL,
  lugar_intercambio_acordado VARCHAR(250) NOT NULL,
  condiciones_uso_acordadas TEXT NOT NULL,
  condiciones_entrega_acordadas TEXT NOT NULL,
  condiciones_devolucion_acordadas TEXT NOT NULL,
  condiciones_cancelacion_acordadas TEXT NOT NULL,
  confirmada_en DATETIME(6) NOT NULL,
  cancelacion_solicitada_en DATETIME(6) NULL,
  cancelada_en DATETIME(6) NULL,
  cancelada_por_usuario_id BINARY(16) NULL,
  motivo_cancelacion TEXT NULL,
  external_event_version BIGINT NOT NULL DEFAULT 0,
  actualizada_en DATETIME(6) NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uq_reservas_solicitud (solicitud_id),
  INDEX idx_reservas_agenda_periodo (agenda_id, estado, desde, hasta),
  CONSTRAINT fk_reservas_solicitud FOREIGN KEY (solicitud_id) REFERENCES solicitudes(id) ON DELETE RESTRICT,
  CONSTRAINT fk_reservas_agenda FOREIGN KEY (agenda_id) REFERENCES agendas_objeto(id) ON DELETE RESTRICT,
  CONSTRAINT ck_reservas_periodo CHECK (desde < hasta),
  CONSTRAINT ck_reservas_tarifa CHECK (tarifa_diaria_acordada > 0),
  CONSTRAINT ck_reservas_garantia CHECK (garantia_monetaria_acordada IS NULL OR garantia_monetaria_acordada >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE outbox_events (
  id BINARY(16) NOT NULL,
  aggregate_type VARCHAR(50) NOT NULL,
  aggregate_id BINARY(16) NOT NULL,
  event_type VARCHAR(100) NOT NULL,
  event_version INT NOT NULL,
  aggregate_version BIGINT NOT NULL,
  payload JSON NOT NULL,
  correlation_id VARCHAR(100) NOT NULL,
  occurred_at DATETIME(6) NOT NULL,
  published_at DATETIME(6) NULL,
  attempts INT NOT NULL DEFAULT 0,
  last_error VARCHAR(500) NULL,
  PRIMARY KEY (id),
  INDEX idx_outbox_pending (published_at, attempts, occurred_at),
  INDEX idx_outbox_aggregate (aggregate_id, aggregate_version)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE processed_events (
  event_id BINARY(16) NOT NULL,
  consumer VARCHAR(100) NOT NULL,
  aggregate_id BINARY(16) NOT NULL,
  aggregate_version BIGINT NOT NULL,
  processed_at DATETIME(6) NOT NULL,
  PRIMARY KEY (event_id),
  INDEX idx_processed_aggregate (aggregate_id, aggregate_version)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE registro_auditoria (
  id BINARY(16) NOT NULL,
  aggregate_type VARCHAR(50) NOT NULL,
  aggregate_id BINARY(16) NOT NULL,
  action VARCHAR(100) NOT NULL,
  previous_state VARCHAR(50) NULL,
  new_state VARCHAR(50) NOT NULL,
  actor_id BINARY(16) NULL,
  correlation_id VARCHAR(100) NOT NULL,
  occurred_at DATETIME(6) NOT NULL,
  PRIMARY KEY (id),
  INDEX idx_audit_aggregate (aggregate_type, aggregate_id, occurred_at),
  INDEX idx_audit_correlation (correlation_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
