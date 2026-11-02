# Pruebas

`mvnw.cmd clean verify` ejecuta:

- dominio: especificaciones componibles, agenda, fábrica, periodos inválidos/consecutivos/solapados, fracciones de segundo, extremos, tarifa, garantía, snapshots, estados y permisos;
- seguridad: token ausente, inválido, correo no verificado, rechazo de rol por header, ausencia de usuarios en memoria y rechazo de HTTP Basic/Form Login;
- contrato: cuerpo de creación, JSON estricto, motivos obligatorios, nueve operaciones, Firebase Bearer y campos esperados por el mapper frontend;
- integración `*IT`: Flyway/MySQL real, dos y veinte aceptaciones concurrentes, creación concurrente de agenda, rollback conjunto reserva/outbox, periodo bloqueado durante la saga, duplicados y eventos fuera de orden;
- RabbitMQ real: publicación con confirm, retorno sin ruta, dos relays concurrentes y marcado posterior; la caída de broker se cubre aislando el relay y verificando que el evento permanece pendiente.

Las pruebas Testcontainers llevan `disabledWithoutDocker=true`: se marcan omitidas cuando Docker no existe. Una omisión no equivale a éxito. La suite automatiza MySQL y RabbitMQ; una prueba end-to-end entre repositorios sigue dependiendo de que Préstamos adopte el contrato publicado.

Última verificación local de esta rama, 2026-11-01: 43 pruebas unitarias y 18 de integración, 61 en total, 0 fallos, 0 errores y 0 omitidas. Se ejecutaron Testcontainers con MySQL 8.4 y RabbitMQ 4.1. Flyway aplicó V1 y V2 desde un esquema vacío. Docker Compose posterior dejó los tres contenedores `healthy`; liveness/readiness respondieron 200. Estos datos no son una prueba de carga.

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

Para RNF05, ejecutar una carga controlada con datos no reales y un token de prueba de corta vida:

```powershell
$env:BASE_URL='http://localhost:8081'
$env:FIREBASE_ID_TOKEN='<token de prueba>'
$env:VUS='20'
$env:DURATION='60s'
k6 run perf/k6-reservations.js
```

El script mide p50/p95/p99 HTTP end-to-end de lectura, errores y concurrencia. Correlacionar con métricas de JVM, Hikari, CPU y memoria del entorno. Para creación/aceptación se necesitan fixtures de Catálogo/Identidad acordados; solo una instrumentación interna puede excluir su latencia. No hay resultados de carga incluidos.
