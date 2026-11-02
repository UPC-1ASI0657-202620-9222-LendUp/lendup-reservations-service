# Contratos de integración

## Confirmados por este repositorio

| Operación | Responsable | Transporte | Datos mínimos | Autenticación | Fallo seguro |
|---|---|---|---|---|---|
| Snapshot de publicación | Catálogo | HTTP GET, plantilla `CATALOG_SNAPSHOT_URI_TEMPLATE` | IDs, estado, `periodo_ofrecido`, tarifa, garantía, moneda, lugar y cuatro condiciones | `CATALOG_SERVICE_TOKEN` | 503, no crea solicitud |
| UID Firebase a UUID | Identidad | HTTP GET, `IDENTITY_FIREBASE_USER_URI_TEMPLATE` | `usuario_id` UUID | `IDENTITY_SERVICE_TOKEN` | 503, no autoriza |
| Contacto autorizado | Identidad | HTTP GET, `IDENTITY_AUTHORIZED_CONTACT_URI_TEMPLATE` | `telefono` | `IDENTITY_SERVICE_TOKEN` | 503, no inventa contacto |
| Cancelación/liberación | Préstamos | RabbitMQ v1 | Contratos en `contracts/events/v1` | Credenciales Rabbit/TLS | retry y DLQ; no libera localmente |

Las rutas concretas quedan externas. Base URL, token y plantilla son obligatorios para cada operación; no se deduce ningún endpoint del monolito.

## Propuestas pendientes de aprobación

### Elegibilidad institucional y términos generales

- Responsable: Identidad.
- Operación requerida: consultar por UUID interno si el estudiante está verificado institucionalmente y qué versión de términos generales aceptó.
- Respuesta mínima propuesta: `usuario_id`, `verificacion_institucional` (`VERIFICADO|NO_VERIFICADO`), `terminos_generales_version`, `terminos_generales_aceptados_en` UTC.
- Autenticación: token de servicio; nunca headers públicos.
- Variable futura: solo después de acordar una URI template con Identidad.
- Fallo: 503 cuando la regla pase a ser obligatoria; `DESCONOCIDO` nunca equivale a verificado.
- Aceptación: pruebas contractuales de verificado, no verificado, ausente, timeout y respuesta inválida.

### Consentimiento de condiciones de publicación

- Responsables: Catálogo y Frontend.
- Operación requerida: Catálogo debe emitir un identificador/version/hash inmutable del snapshot. Frontend debe presentar ese snapshot y enviar una aceptación explícita ligada a él.
- Cuerpo mínimo propuesto, no vigente: `aceptacion_condiciones: { "snapshot_id": "...", "aceptadas": true }`. El servidor registra su propio timestamp; no confía en una fecha del cliente.
- Fallo: 409 si el snapshot cambió y 503 si no puede verificarse.
- Aceptación: prueba E2E que demuestra que el hash presentado es el almacenado y que `condiciones_aceptadas_en` solo aparece con evidencia válida.

Hasta aprobar estas propuestas, el body vigente no cambia y las solicitudes nuevas guardan `consentimiento_verificado=false`. Esto conserva compatibilidad sin presentar `creada_en` como consentimiento.

## Equipos externos

- Préstamos debe producir `PrestamoCancelado`, `CancelacionRechazada` y `PeriodoPrestamoLiberado`, y consumir `ReservaConfirmada`/`CancelacionReservaSolicitada` de forma idempotente.
- Notificaciones puede consumir los eventos emitidos; Reservas no escribe su base.
- Pagos, Garantías y Reputación no tienen una operación de Reservas y no deben acoplarse a sus tablas.
- Gateway debe preservar las dos rutas, `Authorization` y `X-Correlation-ID`.
- DevOps debe inyectar secretos, TLS, alertas Outbox/DLQ y ejecutar Flyway de forma controlada.
