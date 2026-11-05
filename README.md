# LendUp Reservations Service

Microservicio Java 21/Spring Boot 4 para el bounded context de solicitudes, reservas y agendas. Conserva las nueve rutas consumidas por el frontend, usa MySQL/InnoDB con Flyway, Firebase ID tokens, RabbitMQ y transactional outbox. La alineación DDD se concentra en `SolicitudSpecifications`, `AgendaDelObjeto` y `ReservaFactory`; JPA sigue siendo un adaptador.

## Estado y límites

El código es ejecutable, pero crear solicitudes y consultar contactos exige contratos internos que todavía no existen en los repositorios inspeccionados: snapshot autoritativo versionado de Catálogo, elegibilidad institucional/términos de Identidad, vigencia del préstamo y contacto autorizado. Si faltan, las operaciones dependientes responden `503`; no existen stubs de producción.

Las rutas internas no están codificadas con valores supuestos. Deben configurarse mediante las plantillas `CATALOG_*`, `IDENTITY_*` y `LOAN_*` una vez aprobadas. La verificación institucional y términos se modelan como `DESCONOCIDO`, nunca como verificados; por ello hoy la creación falla de forma segura hasta conectar Identidad y Catálogo. Véase [docs/cross-service-integration.md](docs/cross-service-integration.md).

Se inspeccionaron sin modificar `lendup-backend` y `lendup-frontend-web` en las rutas locales proporcionadas. No se encontró el documento final de arquitectura; por tanto, no se atribuyen a él citas ni cumplimiento no verificado. Véase [docs/architecture.md](docs/architecture.md).

## Inicio local

Requisitos: JDK 21, Docker con Compose v2 y credencial Firebase de desarrollo válida. El archivo
`.secrets/firebase-service-account.json` debe pertenecer a `lendup-desarrollo`; el servicio rechaza
la configuración y los tokens del proyecto anterior `lendup-3cb42`.

```powershell
Copy-Item .env.example .env
New-Item -ItemType Directory -Force .secrets
# Colocar una credencial de desarrollo en .secrets/firebase-service-account.json; nunca versionarla.
.\mvnw.cmd clean verify
docker compose up --build
```

API: `http://localhost:8081/api/v1`; readiness: `http://localhost:8081/actuator/health/readiness`. Swagger está deshabilitado por defecto. El `compose.yaml` base fija ambas opciones a `false`, incluso si el `.env` local contiene otros valores. Para desarrollo puede habilitarse expresamente al ejecutar el JAR/contenedor con `SPRINGDOC_ENABLED=true`; sigue requiriendo Firebase salvo que también se active `SWAGGER_PUBLIC=true` fuera de `prod`. Con ambas opciones, Swagger muestra `Authorize` mediante `firebaseBearer` y envía `Authorization: Bearer <token>`. Un override Compose local para habilitarlo no debe versionarse; `SWAGGER_PUBLIC` no debe habilitarse en producción.

Para ejecutar solo el artefacto:

```powershell
.\mvnw.cmd clean package
java -jar target\lendup-reservations-service-0.2.0-SNAPSHOT.jar
```

La zona de los periodos REST es `America/Lima` y se representa sin offset, por compatibilidad con `toApiLocalDateTime`/`fromApiLocalDateTime`. Timestamps de auditoría/eventos se emiten en UTC.

La creación requiere `aceptacion_condiciones` con `snapshot_id`, el SHA-256 `snapshot_hash` y `aceptada: true`. El servidor coteja la referencia con Catálogo y registra su propio instante. El frontend actual debe añadir esta estructura; no se conserva una vía insegura para atribuir consentimiento sin versión/hash.

## Imagen

```powershell
docker build --platform linux/amd64 -t lendup-reservations-service:local .
docker run --rm -p 8081:8081 --env-file .env -v "${PWD}\.secrets\firebase-service-account.json:/run/secrets/firebase-service-account.json:ro" lendup-reservations-service:local
docker tag lendup-reservations-service:local DOCKERHUB_USERNAME/lendup-reservations-service:0.2.0
docker login -u DOCKERHUB_USERNAME
docker push DOCKERHUB_USERNAME/lendup-reservations-service:0.2.0
docker pull DOCKERHUB_USERNAME/lendup-reservations-service:0.2.0
docker inspect --format='{{index .RepoDigests 0}}' DOCKERHUB_USERNAME/lendup-reservations-service:0.2.0
```

No ejecutar `docker push` con un namespace literal; sustituir `DOCKERHUB_USERNAME`. `v0.1.0` y la imagen publicada `:0.1.0` son inmutables; esta rama prepara `0.2.0-SNAPSHOT`. La publicación normal se realiza por tag `v*.*.*` mediante GitHub Actions.
