ALTER TABLE reservas
  DROP CHECK ck_reservas_ocupacion;

ALTER TABLE reservas
  ADD CONSTRAINT ck_reservas_ocupacion CHECK (
    ocupacion_desde = desde AND
    ocupacion_hasta > ocupacion_desde AND
    (
      periodo_liberado_desde IS NULL OR
      periodo_liberado_desde = ocupacion_hasta
    )
  );