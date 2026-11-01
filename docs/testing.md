# Pruebas

`mvnw.cmd clean verify` ejecuta:

- dominio: periodos inválidos, contacto sin solape, solape, bloques de 24 h, tarifa, garantía opcional, estados y permisos;
- seguridad: token ausente, inválido, correo no verificado, rechazo de rol por header, ausencia de usuarios en memoria y rechazo de HTTP Basic/Form Login;
- contrato: cuerpo de creación y campos esperados por el mapper frontend;
- integración `*IT`: Flyway/MySQL real, dos y veinte aceptaciones concurrentes, creación concurrente de agenda, rollback conjunto reserva/outbox, periodo bloqueado durante la saga, duplicados y eventos fuera de orden;
- RabbitMQ real: publicación outbox con publisher confirm y marcado posterior; la caída de broker se cubre aislando el relay y verificando que el evento permanece pendiente.

Las pruebas Testcontainers llevan `disabledWithoutDocker=true`: se marcan omitidas cuando Docker no existe. Una omisión no equivale a éxito. La suite automatiza MySQL y RabbitMQ; una prueba end-to-end entre repositorios sigue dependiendo de que Préstamos adopte el contrato publicado.

La verificación local de seguridad debe confirmar además que solo liveness/readiness son públicas, que Swagger devuelve `401` mientras está deshabilitado/protegido y que la imagen corre como UID/GID 10001.

Prueba manual mínima:

```powershell
.\mvnw.cmd clean verify
docker compose up --build -d
docker compose ps
Invoke-RestMethod http://localhost:8081/actuator/health/liveness
Invoke-RestMethod http://localhost:8081/actuator/health/readiness
docker compose logs reservations --tail 100
docker compose down
```

Para RNF05, ejecutar una carga controlada con datos no reales, reportar p50/p95/p99, concurrencia, hardware y excluir explícitamente el tiempo de Catálogo solo cuando se mida procesamiento interno. No hay resultados de carga incluidos.
