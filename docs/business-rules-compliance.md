# Cumplimiento de reglas de negocio

| Regla | Implementación/verificación | Estado |
|---|---|---|
| Estudiante autenticado, correo e institución verificados, términos vigentes | Firebase real + `SolicitudEligibilityPort`; desconocido falla 503 | Interno completo; Identidad pendiente |
| Consentimiento ligado a condiciones concretas | ID+SHA-256 cotejado con Catálogo, timestamp del servidor, V3 | Interno completo; Frontend/Catálogo pendientes |
| Publicación activa ajena y periodo ofrecido | specification + snapshot; propietario no puede pedir | Interno completo; Catálogo pendiente |
| Pendiente no bloquea; aceptación única crea reserva | agenda solo contiene reservas; solicitud→reserva única | Cumple |
| Intervalos `[desde,hasta)` y 20 aceptaciones | agregado + lock InnoDB + reconsulta; consecutivos permitidos | Cumple, Testcontainers |
| Snapshot económico/textual inmutable | value objects, fábrica y copia en solicitud/reserva | Cumple |
| Duplicado pendiente concurrente | clave SHA-256 nullable solo en `PENDIENTE` + unique | Cumple, Testcontainers |
| Cancelación distribuida | `CANCELACION_PENDIENTE` bloquea hasta decisión de Préstamos | Cumple internamente; productor externo pendiente |
| Criterio vigente antes de recepción | Préstamos decide; Reservas no infiere ni libera | Cumple; contradicción antigua “antes de entrega” documentada |
| Liberación anticipada | prueba autoritativa, sin incidencias, ocupación separada del contrato | Cumple internamente; evento externo pendiente |
| Permisos contextuales | prestatario cancela; prestamista acepta/rechaza; admin según caso explícito | Cumple |
| Privacidad de contacto | solo participante y vigencia autorizada por Préstamos; luego Identidad | Interno completo; dos contratos pendientes |
| Pagos/garantías/devolución física | solo snapshot monetario; sin cobros, reembolsos ni acciones físicas | Fuera de alcance correctamente |

Las condiciones desconocidas nunca se aprueban. Las dependencias obligatorias no disponibles producen errores seguros, no datos inventados.
