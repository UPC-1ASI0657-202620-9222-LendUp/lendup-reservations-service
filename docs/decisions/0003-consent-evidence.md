# ADR 0003: evidencia de consentimiento compatible

Estado: aceptada.

El contrato v1 no contiene una declaración explícita ni una versión de snapshot. Por ello `creada_en` no puede usarse como prueba de consentimiento. Flyway V2 conserva los datos históricos, vuelve nullable `condiciones_aceptadas_en` y agrega `consentimiento_verificado=false`. Las respuestas ocultan timestamps no verificados. La exigencia completa se activará únicamente después de acordar con Identidad, Catálogo y Frontend los contratos descritos en `docs/integration-contracts.md`; `DESCONOCIDO` nunca equivale a aceptación.
