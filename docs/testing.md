# Pruebas

`mvnw.cmd clean verify` ejecuta:

- dominio: especificaciones componibles, agenda, fábrica, periodos inválidos/consecutivos/solapados, fracciones de segundo, extremos, tarifa, garantía, snapshots, estados y permisos;
- seguridad: token ausente, inválido, correo no verificado, audiencia distinta —incluido `lendup-3cb42`—, rechazo de rol por header, ausencia de usuarios en memoria y rechazo de HTTP Basic/Form Login;
- contrato: cuerpo de creación, JSON estricto, motivos obligatorios, nueve operaciones, Firebase Bearer y campos esperados por el mapper frontend;
- integración `*IT`: Flyway/MySQL real, dos y veinte aceptaciones concurrentes, creación concurrente de agenda, rollback conjunto reserva/outbox, periodo bloqueado durante la saga, duplicados y eventos fuera de orden;
- RabbitMQ real: publicación con confirm, retorno sin ruta, dos relays concurrentes y marcado posterior; la caída de broker se cubre aislando el relay y verificando que el evento permanece pendiente.

Las pruebas Testcontainers llevan `disabledWithoutDocker=true`: se marcan omitidas cuando Docker no existe. Una omisión no equivale a éxito. La suite automatiza MySQL y RabbitMQ; una prueba end-to-end entre repositorios sigue dependiendo de que Préstamos adopte el contrato publicado.

Última verificación Maven local de esta rama, 2026-11-01: 53 pruebas unitarias y 20 de integración, 73 en total, 0 fallos, 0 errores y 0 omitidas. Se ejecutaron Testcontainers con MySQL 8.4 y RabbitMQ 4.1. Flyway aplicó V1–V3 desde cero y una prueba independiente migró datos históricos desde V1 verificando integridad y ausencia de consentimiento ficticio. La verificación Docker posterior se registra separadamente; estos datos no son una prueba de carga.

La imagen local se construyó el mismo día con `--platform linux/amd64`. Compose dejó MySQL, RabbitMQ y Reservas `healthy`; Flyway confirmó V1, V2 y V3. Liveness/readiness devolvieron `UP`. Actuator general, las rutas de negocio, Swagger/API docs, un bearer inválido y HTTP Basic devolvieron 401 sin challenge Basic. El Compose base fija `SPRINGDOC_ENABLED=false` y `SWAGGER_PUBLIC=false`, independientemente del `.env` local. El contenedor se verificó como UID/GID 10001, root filesystem de solo lectura, `cap_drop=ALL` y `no-new-privileges`.

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
