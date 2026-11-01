# ADR 0002: cancelación dirigida por Préstamos

Estado: aceptada.

Reservas no decide de forma definitiva a partir de una lectura potencialmente obsoleta. La petición cambia a `CANCELACION_PENDIENTE` y conserva el intervalo; Préstamos responde por evento autoritativo. La deduplicación por `event_id` y el orden por `aggregate_version` hacen los reintentos seguros.
