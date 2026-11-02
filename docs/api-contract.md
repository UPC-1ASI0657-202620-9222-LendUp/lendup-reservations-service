# Contrato API

Base externa: `/api/v1`. Todos los endpoints requieren `Authorization: Bearer <Firebase ID token>` y usan JSON `snake_case`.

| Método | Ruta | Resultado |
|---|---|---|
| POST | `/solicitudes` | 201, solicitud con snapshot de Catálogo; 409 si ya existe una solicitud pendiente idéntica o el periodo ya está comprometido localmente |
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

Los motivos de rechazo y cancelación son cadenas obligatorias, no blancas, de hasta 1000 caracteres. JSON es estricto: propiedades desconocidas, incluidos UUID o roles aportados por el cliente, producen 400. La respuesta conserva `condiciones_aceptadas_en`, pero es `null` mientras no exista evidencia explícita y verificable; `creada_en` nunca se reutiliza como consentimiento.

OpenAPI define globalmente `firebaseBearer` (`type: http`, `scheme: bearer`, `bearerFormat: JWT`) para las nueve operaciones. La UI solo se habilita con `SPRINGDOC_ENABLED=true`; su acceso público exige además `SWAGGER_PUBLIC=true` y se limita a desarrollo.

## Contratos internos mínimos pendientes

Los repositorios inspeccionados no contienen rutas internas acordadas para Catálogo o Identidad. Por ello el servicio no asume paths: las plantillas URI quedan vacías por defecto y cualquier operación dependiente responde `503` hasta que los equipos responsables configuren contratos reales.

Catálogo debe acordar `CATALOG_SNAPSHOT_URI_TEMPLATE`, con tres variables posicionales en este orden: publicación, `desde` y `hasta`. La respuesta requerida contiene `id`, `propietario_usuario_id`, `estado`, `periodo_ofrecido`, tarifa, garantía opcional, moneda, lugar y cuatro condiciones. Catálogo calcula `periodo_ofrecido` autoritativamente; al aceptar, la agenda local sigue siendo la exclusión definitiva.

Identidad debe acordar:

- `IDENTITY_FIREBASE_USER_URI_TEMPLATE`, con el Firebase UID como única variable, y respuesta `{ "usuario_id": "UUID" }`;
- `IDENTITY_AUTHORIZED_CONTACT_URI_TEMPLATE`, con usuario y reserva como variables posicionales, y respuesta `{ "telefono": "..." }`.

Ambas llamadas usan token de servicio, timeouts acotados y fallo seguro. Reservas no acepta UID, UUID, roles ni teléfono aportados por el body o por headers públicos.

La verificación institucional y términos generales requieren una operación de Identidad aún no confirmada. La aceptación concreta de la publicación requiere que frontend envíe una declaración explícita ligada a una versión/hash de snapshot que Catálogo pueda validar. Hasta aprobar ambos contratos, Reservas conserva compatibilidad sin afirmar consentimiento y no habilita un bypass. Detalle y criterios en `docs/integration-contracts.md`.
