# Integración entre servicios

Los paths siguientes son plantillas configurables y propuestas pendientes de aprobación salvo que se indique lo contrario. Todos los HTTP internos requieren bearer token de servicio, TLS y timeouts; Reservas responde 503 ante ausencia, timeout o contrato inválido.

| Responsable | Operación/transporte | Esquema requerido | Variables | Error y aceptación |
|---|---|---|---|---|
| Identidad | GET UID → UUID | `{usuario_id: UUID}` | `IDENTITY_SERVICE_BASE_URL`, `IDENTITY_FIREBASE_USER_URI_TEMPLATE`, token | 503; probar UID ausente/inválido |
| Identidad | GET elegibilidad | `{estado_verificacion: VERIFICADO|NO_VERIFICADO, terminos_generales_vigentes_aceptados: boolean}` | `IDENTITY_ELIGIBILITY_URI_TEMPLATE` | desconocido 503, negativo 403; probar versión vigente |
| Identidad | GET contacto autorizado | `{telefono: string}` | `IDENTITY_AUTHORIZED_CONTACT_URI_TEMPLATE` | 503 sin filtrar datos; probar contraparte correcta |
| Catálogo | GET snapshot por publicación/periodo | ID, propietario, estado, `periodo_ofrecido`, `snapshot_id`, SHA-256, importes, moneda, lugar y cuatro condiciones | `CATALOG_SERVICE_BASE_URL`, `CATALOG_SNAPSHOT_URI_TEMPLATE`, token | 409 snapshot obsoleto, 503 fallo; canonicalización estable |
| Préstamos | GET vigencia contacto | `{contacto_vigente: boolean}` para reserva y participante | `LOAN_SERVICE_BASE_URL`, `LOAN_CONTACT_ACCESS_URI_TEMPLATE`, token | 403/503; probar cierre/activación y admin ajeno |
| Préstamos | RabbitMQ consume | `ReservaConfirmada`, `CancelacionReservaSolicitada` | exchange/routing v1 | creación y decisión idempotentes por `event_id` |
| Préstamos | RabbitMQ produce | `PrestamoCancelado`, `CancelacionRechazada`, `PeriodoPrestamoLiberado` | contrato JSON v1 | Inbox, versión, retry/DLQ; probar evidencia de devolución |
| Notificaciones | RabbitMQ consume | seis eventos emitidos, incluidos rechazo/cancelación de solicitud | bindings v1 | consumidor idempotente; Reservas no envía correo |
| Pagos/Garantías | consume datos económicos desde flujo acordado | importes/moneda del snapshot, sin tarjeta | por definir por su equipo | no hay operación de cobro en Reservas |
| Frontend | POST solicitud | añade `aceptacion_condiciones` del snapshot mostrado | base Gateway existente | prueba E2E de hash obsoleto y checkbox |
| API Gateway | proxy REST | preserva `/api/v1`, `Authorization`, JSON y `X-Correlation-ID` | routing/timeout | nueve rutas y 401/403/503 sin reescritura |
| DevOps | MySQL/Rabbit/Firebase/secretos | variables de `docs/aws-handoff.md` | Secrets Manager/Parameter Store | smoke test, TLS, alertas Outbox/DLQ |

## Compatibilidad observada

El frontend mantiene las nueve rutas, snake_case y fechas locales `America/Lima`, pero aún no transmite ID/hash. El backend monolítico no ofrece el snapshot/elegibilidad/contacto-vigente con estos contratos y crea Préstamos directamente. Por tanto, el código de Reservas está preparado y falla de forma segura, pero la integración end-to-end no está aprobada ni probada.

Catálogo no debe leer la base privada de Reservas. La ocupación puede propagarse por eventos acordados, mientras que la exclusión definitiva continúa bajo el lock de agenda de Reservas. Préstamos no debe escribir tablas de Reservas; sus decisiones llegan por eventos v1.
