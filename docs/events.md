# Eventos

Exchange durable: `lendup.domain.events` (topic). DLX: `lendup.domain.events.dlx`. Cola consumidora: `reservations.loan-events.v1`; DLQ: `reservations.loan-events.v1.dlq`.

| Dirección | Evento | Routing key | Propósito |
|---|---|---|---|
| Emite | `SolicitudCreada` v1 | `reservas.solicitud-creada.v1` | Notificaciones/proyecciones |
| Emite | `SolicitudRechazada` v1 | `reservas.solicitud-rechazada.v1` | Notificaciones/proyecciones |
| Emite | `SolicitudCancelada` v1 | `reservas.solicitud-cancelada.v1` | Notificaciones/proyecciones |
| Emite | `ReservaConfirmada` v1 | `reservas.confirmada.v1` | Préstamos crea su agregado con snapshot |
| Emite | `CancelacionReservaSolicitada` v1 | `reservas.cancelacion-solicitada.v1` | Préstamos decide según recepción/estado físico |
| Emite | `ReservaCancelada` v1 | `reservas.cancelada.v1` | Proyecciones/notificaciones |
| Consume | `PrestamoCancelado` v1 | `prestamos.cancelado.v1` | Cancela y libera periodo |
| Consume | `CancelacionRechazada` v1 | `prestamos.cancelacion-rechazada.v1` | Conserva periodo y vuelve a confirmada |
| Consume | `PeriodoPrestamoLiberado` v1 | `prestamos.periodo-liberado.v1` | Acorta futuro ocupado, idempotente |

Envelope: `event_id`, `event_type`, `event_version`, `aggregate_version`, `aggregate_id`, `occurred_at` UTC, `correlation_id`, `payload`. Schemas y ejemplos están en `contracts/events/v1`.

El relay reclama con una transacción corta y `FOR UPDATE SKIP LOCKED`; publica fuera del bloqueo y confirma el resultado en otra transacción. Un lease vencido recupera filas tras caídas. Exige routing y publisher confirm, respeta el orden por agregado y aplica backoff acotado. Tras diez intentos por defecto queda agotado para intervención operativa; no se borra. Inbox reclama cada `event_id` atómicamente, valida versión/tipo/payload antes de mutar, y `external_event_version` evita retrocesos. Retry del listener es 3 y, al agotarse, RabbitMQ enruta a `reservations.loan-events.v1.dlq`.

`PeriodoPrestamoLiberado` solo es válido con `decision=LIBERACION_CONFIRMADA`, `devolucion_confirmada_por_prestamista=true` e `incidencias_pendientes=false`. La aplicación acorta la ocupación, conserva el periodo contractual y rechaza fechas arbitrarias, duplicados y versiones atrasadas.

Los consumidores de Préstamos y Notificaciones del monolito inspeccionado todavía no implementan estos contratos; publicar no constituye una integración end-to-end exitosa.
