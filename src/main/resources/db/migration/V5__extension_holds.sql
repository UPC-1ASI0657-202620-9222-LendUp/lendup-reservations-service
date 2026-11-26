CREATE TABLE extension_holds (
  change_id CHAR(36) PRIMARY KEY,
  publication_id CHAR(36) NOT NULL,
  reservation_id CHAR(36) NOT NULL,
  loan_id CHAR(36) NOT NULL,
  starts_at DATETIME(6) NOT NULL,
  ends_at DATETIME(6) NOT NULL,
  expires_at DATETIME(6) NOT NULL,
  status VARCHAR(16) NOT NULL,
  CONSTRAINT ck_extension_hold_period CHECK (starts_at < ends_at),
  CONSTRAINT ck_extension_hold_status CHECK (status IN ('RETENIDA','CONFIRMADA','RECHAZADA')),
  INDEX ix_extension_hold_agenda (publication_id, status, expires_at),
  INDEX ix_extension_hold_expiry (status, expires_at)
) ENGINE=InnoDB;
