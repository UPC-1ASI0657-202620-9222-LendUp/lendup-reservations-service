# Contrato API

Base externa: `/api/v1`. Todos los endpoints requieren `Authorization: Bearer <Firebase ID token>` y usan JSON `snake_case`.

| Método | Ruta | Resultado |
|---|---|---|
| POST | `/solicitudes` | 201, solicitud con snapshot de Catálogo |
| GET | `/solicitudes?rol=&estado=` | 200, solicitudes autorizadas |
| GET | `/solicitudes/{id}` | 200, participante/admin |
| POST | `/solicitudes/{id}/aceptacion` | 201, reserva; 409 ante estado/solapamiento |
| POST | `/solicitudes/{id}/rechazo` | 200 |
| POST | `/solicitudes/{id}/cancelacion` | 200 |
| GET | `/reservas?rol=` | 200 |
| POST | `/reservas/{id}/cancelacion` | 200 con estado `CANCELACION_PENDIENTE` |
| GET | `/reservas/{id}/contacto` | 200 `{ "telefono": "..." }` o 503 |

Errores: `{ "status": 409, "error": "CONFLICTO_NEGOCIO", "message": "...", "timestamp": "...Z" }`. Códigos previstos: 400, 401, 403, 404, 409 y 503.

Los campos históricos consumidos por `services/api/mappers/domain.ts` se preservan. Los periodos `desde`/`hasta` son `LocalDateTime` de negocio en `America/Lima`; timestamps terminados en `_en` son instantes UTC. El estado nuevo `CANCELACION_PENDIENTE` debe mapearse visualmente como reserva confirmada con cancelación en trámite hasta que el frontend soporte una etiqueta propia.

## Contratos internos mínimos pendientes

Los repositorios inspeccionados no contienen rutas internas acordadas para Catálogo o Identidad. Por ello el servicio no asume paths: las plantillas URI quedan vacías por defecto y cualquier operación dependiente responde `503` hasta que los equipos responsables configuren contratos reales.

Catálogo debe acordar `CATALOG_SNAPSHOT_URI_TEMPLATE`, con tres variables posicionales en este orden: publicación, `desde` y `hasta`. La respuesta requerida contiene `id`, `propietario_usuario_id`, `estado`, `periodo_ofrecido`, tarifa, garantía opcional, moneda, lugar y cuatro condiciones. Catálogo calcula `periodo_ofrecido` autoritativamente; al aceptar, la agenda local sigue siendo la exclusión definitiva.

Identidad debe acordar:

- `IDENTITY_FIREBASE_USER_URI_TEMPLATE`, con el Firebase UID como única variable, y respuesta `{ "usuario_id": "UUID" }`;
- `IDENTITY_AUTHORIZED_CONTACT_URI_TEMPLATE`, con usuario y reserva como variables posicionales, y respuesta `{ "telefono": "..." }`.

Ambas llamadas usan token de servicio, timeouts acotados y fallo seguro. Reservas no acepta UID, UUID, roles ni teléfono aportados por el body o por headers públicos.
