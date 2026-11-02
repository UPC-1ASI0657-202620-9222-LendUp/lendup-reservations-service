# ADR 0004: relay Outbox multiinstancia

Estado: aceptada.

Cada relay reclama hasta 50 filas pendientes mediante `SELECT ... FOR UPDATE SKIP LOCKED`, publica con flag obligatorio y publisher confirms, y marca la fila dentro de la misma transacción. Esto evita trabajo simultáneo sobre una fila entre instancias. Un crash después del publish y antes del commit todavía puede duplicar: la garantía es al menos una vez. Inbox usa un `INSERT IGNORE` transaccional sobre `event_id`; el productor y consumidor mantienen `aggregate_version` monotónica para descartar eventos atrasados.
