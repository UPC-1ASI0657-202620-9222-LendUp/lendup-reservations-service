# Contratos de integración

Este documento separa lo confirmado por código público inspeccionado de las propuestas necesarias para operar el microservicio. Reservas no trae endpoints ficticios ni respuestas de éxito simuladas; una plantilla ausente, timeout o respuesta incompleta produce `503`.

## Confirmado en los repositorios inspeccionados

- Firebase autentica y entrega UID, `email_verified` y custom claims firmadas.
- El backend de Identidad puede resolver usuarios, pero no expone un contrato interno confirmado que combine UUID, verificación institucional y aceptación de la versión vigente de términos.
- Catálogo expone publicación, condiciones y disponibilidades, pero su contrato público no incluye `snapshot_id`, hash autoritativo ni `periodo_ofrecido` con la forma requerida por Reservas.
- El frontend muestra una casilla de condiciones, pero `requests.service.ts` no envía evidencia ligada al snapshot.
- El monolito crea Préstamos directamente; no confirma los contratos de eventos ni la consulta de vigencia de contacto del servicio independiente.

## Propuestas implementadas como puertos configurables

| Operación | Responsable | Configuración | Respuesta mínima | Fallo |
|---|---|---|---|---|
| UID Firebase → UUID | Identidad | `IDENTITY_FIREBASE_USER_URI_TEMPLATE` | `usuario_id` UUID | 503/401; no autoriza |
| Elegibilidad | Identidad | `IDENTITY_ELIGIBILITY_URI_TEMPLATE` | `estado_verificacion: VERIFICADO`, `terminos_generales_vigentes_aceptados: true` | desconocido 503; negativo 403 |
| Snapshot de publicación | Catálogo | `CATALOG_SNAPSHOT_URI_TEMPLATE` | ID coincidente, `snapshot_id`, SHA-256, propietario, estado, periodo, importes y cuatro condiciones | 503; obsoleto 409 |
| Vigencia del contacto | Préstamos | `LOAN_CONTACT_ACCESS_URI_TEMPLATE` | `contacto_vigente: true` para reserva/participante | 503/403; no consulta teléfono |
| Teléfono autorizado | Identidad | `IDENTITY_AUTHORIZED_CONTACT_URI_TEMPLATE` | `telefono` solo tras autorización anterior | 503; no inventa dato |

Todas las llamadas usan base URL, bearer token de servicio y timeouts externos. Los tokens se inyectan como secretos; no se aceptan UID, UUID, rol, permiso o teléfono desde headers públicos o cuerpos.

## Criterios de aceptación externos

Identidad debe probar elegible, no elegible, términos no aceptados, usuario ausente, timeout y respuesta inválida. Catálogo debe demostrar canonicalización/hash estable, ID solicitado coincidente, versión obsoleta, disponibilidad y snapshot inmutable. Préstamos debe demostrar que el acceso al contacto termina al cerrar la operación y que un administrador no participante no se habilita por defecto. Frontend debe enviar exactamente `aceptacion_condiciones: {snapshot_id, snapshot_hash, aceptada:true}` obtenido del snapshot mostrado.

Los eventos confirmados por este repositorio están en `contracts/events/v1`. Su adopción por Préstamos y Notificaciones sigue pendiente; publicar a RabbitMQ no demuestra integración extremo a extremo.
