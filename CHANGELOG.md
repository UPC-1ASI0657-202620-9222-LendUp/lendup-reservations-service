# Changelog

## Unreleased

Versión de desarrollo: `0.2.0-SNAPSHOT`. El tag y la imagen `v0.1.0` permanecen sin cambios.

- Servicio inicial de solicitudes y reservas con arquitectura hexagonal.
- Exclusión mutua pesimista para agendas y protección de solapamientos.
- Saga de cancelación, outbox/inbox, contratos REST/eventos, seguridad Firebase y entrega Docker/AWS.
- Endurecimiento previo a publicación: sin HTTP Basic/Form Login ni usuario en memoria, Actuator mínimo, Swagger deshabilitado por defecto, contratos internos configurables y contenedor de solo lectura/no root.
- Alineación DDD con `SolicitudSpecifications`, política de intervalos en `AgendaDelObjeto` y creación mediante `ReservaFactory`.
- Corrección del redondeo de bloques iniciados de 24 horas para fracciones de segundo y validación de snapshots compatible con MySQL.
- Flyway V2 no destructiva para distinguir evidencia de consentimiento; el contrato vigente deja el timestamp en `null` en vez de inventarlo.
- OpenAPI con `firebaseBearer`, JSON estricto y motivos obligatorios sin alterar las nueve rutas.
- Outbox multiinstancia con `SKIP LOCKED`, detección de mensajes no enrutados e Inbox con reclamo idempotente atómico.
- Separación hexagonal completa: aplicación y puertos ya no exponen entidades JPA; ArchUnit protege las dependencias.
- Flyway V3 agrega consentimiento verificable, deduplicación atómica de solicitudes pendientes, periodo de ocupación separado del contractual y leasing del Outbox.
- Creación segura con verificación institucional/términos, snapshot ID+SHA-256 y timestamp de aceptación del servidor; dependencias desconocidas producen 503.
- Liberación anticipada autoritativa sin sobrescribir `desde`/`hasta`, y privacidad de contactos validada por Préstamos antes de consultar Identidad.
- Eventos v1 `SolicitudRechazada` y `SolicitudCancelada`; `ReservaConfirmada` incluye referencia verificable del snapshot.
