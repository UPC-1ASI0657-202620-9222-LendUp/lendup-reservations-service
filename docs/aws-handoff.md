# Entrega a AWS

## Artefacto

Imagen estable existente e inmutable: `fabriciofabianquispebarzola/lendup-reservations-service:0.1.0`. Esta rama genera `0.2.0-SNAPSHOT`; todavía no se publicó ni se asignó digest remoto. La siguiente entrega debe usar `fabriciofabianquispebarzola/lendup-reservations-service:<nuevo-semver|sha>` sin sobrescribir `0.1.0`. Plataforma actual `linux/amd64`; puerto 8081; liveness `/actuator/health/liveness`; readiness `/actuator/health/readiness`. Registrar tag y digest inmutable después de publicar. Para Graviton, añadir `linux/arm64` a `platforms` del workflow, verificar dependencias y publicar manifest multi-arquitectura.

## Configuración

Obligatorias: `SPRING_PROFILES_ACTIVE=prod`, `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `RABBITMQ_HOST`, `RABBITMQ_PORT`, `RABBITMQ_USERNAME`, `RABBITMQ_PASSWORD`, `FIREBASE_PROJECT_ID=lendup-desarrollo`, ADC Firebase externo del mismo proyecto, `CORS_ORIGINS`, `CATALOG_SERVICE_BASE_URL`, `CATALOG_SERVICE_TOKEN`, `CATALOG_SNAPSHOT_URI_TEMPLATE`, `IDENTITY_SERVICE_BASE_URL`, `IDENTITY_SERVICE_TOKEN`, `IDENTITY_FIREBASE_USER_URI_TEMPLATE`, `IDENTITY_ELIGIBILITY_URI_TEMPLATE`, `IDENTITY_AUTHORIZED_CONTACT_URI_TEMPLATE`, `LOAN_SERVICE_BASE_URL`, `LOAN_SERVICE_TOKEN` y `LOAN_CONTACT_ACCESS_URI_TEMPLATE`. Las plantillas deben proceder de contratos acordados; el servicio no trae endpoints internos ficticios. No usar credenciales ni tokens de `lendup-3cb42`; el SDK valida audiencia, emisor y revocación contra el proyecto compartido.

Opcionales: `SERVER_PORT=8081`, `DB_POOL_MAX_SIZE=25` (mínimo 21 para la prueba objetivo de 20 aceptaciones simultáneas), timeouts `*_TIMEOUT_MS`, `OUTBOX_RELAY_DELAY_MS=1000` y `OUTBOX_MAX_ATTEMPTS=10`. Mantener `SPRINGDOC_ENABLED=false` y `SWAGGER_PUBLIC=false` en producción. Ningún valor secreto debe ir en la definición de imagen, Git o logs. Usar Secrets Manager/Parameter Store e IAM Task Role/IRSA según la plataforma elegida.

MySQL escucha 3306 y debe ser MySQL 8+ InnoDB con base `lendup_reservas` y usuario limitado a ese esquema. RabbitMQ escucha 5672 sobre TLS en producción; 15672 no debe exponerse públicamente. El servicio, DB y broker deben estar en subredes privadas; solo Gateway/ALB llega al 8081.

La imagen ejecuta como UID/GID 10001, no contiene credenciales y soporta filesystem raíz de solo lectura con `/tmp` temporal. En ECS/EKS mantener `readOnlyRootFilesystem`, eliminar capabilities Linux y usar `no-new-privileges` o su equivalente. Inyectar ADC Firebase y tokens de servicio en runtime; no usar build args, variables de imagen ni archivos copiados.

Gateway debe enrutar sin reescritura de cuerpos:

- `/api/v1/solicitudes` y subrutas;
- `/api/v1/reservas` y subrutas.

Propagar `Authorization` y `X-Correlation-ID`; preservar timeouts superiores a los del servicio.

## Despliegue y rollback

1. Aprovisionar DB/Rabbit/secretos y confirmar contratos internos.
2. Ejecutar Flyway mediante una única tarea/instancia antes de escalar; revisar backup y usuario DDL temporal si el usuario runtime no tiene DDL.
3. Desplegar una instancia, comprobar readiness y Flyway V3 (consentimiento verificable, deduplicación pendiente, ocupación separada y leases Outbox), revisar outbox; luego escalar.
4. Verificar consumo/publicación con IDs sintéticos y métricas/DLQ.
5. Registrar `docker inspect --format='{{index .RepoDigests 0}}' IMAGE:TAG`.

Rollback: detener tráfico, volver al digest previo y no revertir destructivamente Flyway. Las migraciones deben ser compatibles hacia atrás; si una futura migración no lo es, usar expand/contract. Inspeccionar `outbox_events`, DLQ y errores antes de reintentar.

Recursos iniciales orientativos, no dimensionamiento definitivo: 0.5 vCPU, 768 MiB RAM, 2 réplicas, pool DB total menor que el límite de RDS. Ajustar por prueba de carga y métricas p95.

Riesgos bloqueantes: contratos internos de Catálogo/Identidad, verificación institucional/consentimiento versionado, adopción de eventos por Préstamos/Notificaciones, mapeo Gateway, credenciales Firebase, TLS/broker, capacidad/backup RDS y observabilidad. Alertar por filas Outbox con `published_at IS NULL AND attempts >= OUTBOX_MAX_ATTEMPTS` y por mensajes en DLQ. Este repositorio no despliega recursos AWS.
