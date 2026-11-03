# ADR 0004: relay Outbox multiinstancia

Estado: aceptada.

Cada relay reclama hasta 10 filas mediante una transacción corta con `SELECT ... FOR UPDATE SKIP LOCKED`, asigna un lease y confirma. Publica fuera de la transacción usando mandatory routing y publisher confirms; después marca éxito o fallo en otra transacción. Los leases vencidos son recuperables, el backoff es acotado y una fila no adelanta versiones anteriores sin publicar del mismo agregado. Un crash después del publish y antes del marcado puede duplicar: la garantía es al menos una vez. Inbox usa `INSERT IGNORE` transaccional sobre `event_id` y `aggregate_version` monotónica descarta eventos atrasados.
