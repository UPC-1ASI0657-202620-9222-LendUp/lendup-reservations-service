# ADR 0003: evidencia de consentimiento compatible

Estado: aceptada.

El cuerpo de creación requiere `aceptacion_condiciones` con ID de snapshot, SHA-256 y confirmación explícita. Reservas coteja la referencia con Catálogo y registra su propio timestamp; `creada_en` nunca es evidencia. Flyway V2 conserva filas históricas y V3 añade las referencias sin atribuir consentimiento ficticio. La creación falla con 503 si Identidad/Catálogo no pueden acreditar los hechos y con 409 si el snapshot cambió. El frontend debe adoptar el campo nuevo antes del cutover.
