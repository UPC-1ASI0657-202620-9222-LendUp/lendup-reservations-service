# Arquitectura

## Alcance

Reservas posee `solicitudes`, `reservas`, `agendas_objeto`, outbox, inbox y auditoría. Los UUID de publicación y usuarios son referencias externas; la migración no crea claves foráneas a otros contextos. No se leen ni escriben tablas de Identidad, Catálogo, Préstamos, Pagos o Notificaciones.

La organización sigue puertos y adaptadores:

- `domain`: periodo semiabierto, condiciones inmutables, agregados y transiciones.
- `application`: casos de uso y puertos explícitos de persistencia, Catálogo, Identidad, outbox y auditoría.
- `infrastructure`: JPA/MySQL, REST con timeouts, Firebase Admin, RabbitMQ y relay.
- `interfaces/rest`: DTOs explícitos, validación y traducción de errores.

## Concurrencia

Aceptar una solicitud abre una transacción breve: bloquea la solicitud, inicializa de forma idempotente la agenda mediante `INSERT IGNORE` en una transacción independiente y bloquea la agenda con `PESSIMISTIC_WRITE`. Después reconsulta reservas `CONFIRMADA` o `CANCELACION_PENDIENTE` que cumplan `existente.desde < nuevo.hasta AND existente.hasta > nuevo.desde`, inserta una reserva única, actualiza la solicitud y escribe auditoría/outbox. La inicialización independiente evita la carrera de primera creación; el pool por defecto conserva conexiones de margen para 20 aceptaciones simultáneas. No hay HTTP dentro del lock. La restricción única `solicitud_id` evita dos reservas para una solicitud.

## Consistencia distribuida

La confirmación publica `ReservaConfirmada` mediante outbox. La solicitud de cancelación conserva el periodo bloqueado y publica `CancelacionReservaSolicitada`. Solo `PrestamoCancelado` cambia la reserva a `CANCELADA`; `CancelacionRechazada` la devuelve a `CONFIRMADA`. `PeriodoPrestamoLiberado` puede acortar un intervalo futuro. Inbox deduplica `event_id` y `aggregate_version` evita aplicar mensajes atrasados.

RabbitMQ ofrece entrega al menos una vez, no exactamente una vez. El relay usa publisher confirms; errores dejan el evento pendiente con intentos acotados. El consumidor usa retry y DLQ durable.

## Seguridad

Se valida el Firebase ID token con Firebase Admin, incluyendo revocación y `email_verified`. El UID verificado se resuelve a UUID interno mediante el contrato autenticado de Identidad; no se confían IDs del body ni headers públicos. Administración proviene únicamente de custom claims verificadas. En `prod` no hay bypass. Credenciales se entregan como Application Default Credentials/secretos externos.

## Referencias inspeccionadas y brechas

Se inspeccionaron las clases de Reservas, `Store`, `FlowSupport`, `FirebaseTokenFilter`, `FirebaseSecurity`, `schema.sql` y tests del backend local; y `requests.service.ts`, `reservations.service.ts`, `endpoints.ts`, `mappers/domain.ts`, `dates.ts` y el contrato de integración del frontend local. El documento final de arquitectura (RF/RNF/R y figuras/tablas citadas en el encargo) no estaba disponible en el entorno ni en los repositorios cercanos. Estas referencias no fueron modificadas.

El monolito actual accede directamente tablas ajenas y crea Préstamos/Notificaciones; este servicio deliberadamente no lo hace. Los endpoints internos propuestos en `docs/api-contract.md` requieren acuerdo de sus equipos.

## RNF

RNF12 se protege mediante serialización por agenda y se cubre con una prueba Testcontainers de 20 aceptaciones. RNF05 define p95 interno menor o igual a 2 s, excluyendo red externa; no se ejecutó ni se inventa un load test. Debe medirse en un entorno equivalente a producción con percentiles de Actuator/Micrometer.
