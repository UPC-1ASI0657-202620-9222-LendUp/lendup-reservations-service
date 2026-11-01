# LendUp Reservations Service

Microservicio Java 21/Spring Boot 4 para el bounded context de solicitudes, reservas y agendas. Conserva las nueve rutas consumidas por el frontend, usa MySQL/InnoDB con Flyway, Firebase ID tokens, RabbitMQ y transactional outbox.

## Estado y límites

El código es ejecutable, pero una instalación funcional requiere contratos internos que todavía no existen en los repositorios inspeccionados: snapshot autoritativo de Catálogo, contacto autorizado de Identidad y consumidores/productores de Préstamos. Si faltan, las operaciones dependientes responden `503`; no existen stubs de producción.

Las rutas internas de Catálogo e Identidad no están codificadas con valores supuestos. Deben configurarse mediante `CATALOG_SNAPSHOT_URI_TEMPLATE`, `IDENTITY_FIREBASE_USER_URI_TEMPLATE` e `IDENTITY_AUTHORIZED_CONTACT_URI_TEMPLATE` una vez que sus equipos publiquen contratos acordados.

Se inspeccionaron sin modificar `lendup-backend` y `lendup-frontend-web` en las rutas locales proporcionadas. No se encontró el documento final de arquitectura; por tanto, no se atribuyen a él citas ni cumplimiento no verificado. Véase [docs/architecture.md](docs/architecture.md).

## Inicio local

Requisitos: JDK 21, Docker con Compose v2 y credencial Firebase de desarrollo válida.

```powershell
Copy-Item .env.example .env
New-Item -ItemType Directory -Force .secrets
# Colocar una credencial de desarrollo en .secrets/firebase-service-account.json; nunca versionarla.
.\mvnw.cmd clean verify
docker compose up --build
```

API: `http://localhost:8081/api/v1`; readiness: `http://localhost:8081/actuator/health/readiness`. Swagger está deshabilitado por defecto; para desarrollo local puede habilitarse expresamente con `SPRINGDOC_ENABLED=true` y requiere Firebase salvo que también se configure `SWAGGER_PUBLIC=true`. Esta última opción no debe habilitarse en producción.

Para ejecutar solo el artefacto:

```powershell
.\mvnw.cmd clean package
java -jar target\lendup-reservations-service-0.1.0-SNAPSHOT.jar
```

La zona de los periodos REST es `America/Lima` y se representa sin offset, por compatibilidad con `toApiLocalDateTime`/`fromApiLocalDateTime`. Timestamps de auditoría/eventos se emiten en UTC.

## Imagen

```powershell
docker build --platform linux/amd64 -t lendup-reservations-service:local .
docker run --rm -p 8081:8081 --env-file .env -v "${PWD}\.secrets\firebase-service-account.json:/run/secrets/firebase-service-account.json:ro" lendup-reservations-service:local
docker tag lendup-reservations-service:local DOCKERHUB_USERNAME/lendup-reservations-service:0.1.0
docker login -u DOCKERHUB_USERNAME
docker push DOCKERHUB_USERNAME/lendup-reservations-service:0.1.0
docker pull DOCKERHUB_USERNAME/lendup-reservations-service:0.1.0
docker inspect --format='{{index .RepoDigests 0}}' DOCKERHUB_USERNAME/lendup-reservations-service:0.1.0
```

No ejecutar `docker push` con un namespace literal; sustituir `DOCKERHUB_USERNAME`. La publicación normal se realiza por tag `v*.*.*` mediante GitHub Actions.
