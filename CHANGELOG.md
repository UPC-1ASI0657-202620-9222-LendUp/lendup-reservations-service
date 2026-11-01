# Changelog

## Unreleased

- Servicio inicial de solicitudes y reservas con arquitectura hexagonal.
- Exclusión mutua pesimista para agendas y protección de solapamientos.
- Saga de cancelación, outbox/inbox, contratos REST/eventos, seguridad Firebase y entrega Docker/AWS.
- Endurecimiento previo a publicación: sin HTTP Basic/Form Login ni usuario en memoria, Actuator mínimo, Swagger deshabilitado por defecto, contratos internos configurables y contenedor de solo lectura/no root.
