# Cumplimiento del diagrama C4

| Componente | Implementación | Responsabilidad efectiva | Dependencias permitidas | Prueba | Estado |
|---|---|---|---|---|---|
| Reservas Controller | `interfaces/rest/ReservationController` | Nueve rutas, DTO/validación, actor autenticado | casos de uso y DTO REST | `FrontendContractTest`, `MySqlConcurrencyIT` | Cumple |
| Application Service | `application/service/ReservationApplicationService` | Orquesta transacciones, autorización, auditoría y eventos | dominio y puertos | `ReservationApplicationServiceTest`, ArchUnit | Cumple |
| SolicitudSpecifications | `domain/specification/SolicitudSpecifications` | Seis reglas puras y componibles | solo dominio/Java | `SolicitudSpecificationsTest` | Cumple |
| AgendaDelObjeto | `domain/model/AgendaDelObjeto` | Política semiabierta y periodos bloqueantes | solo dominio/Java | `AgendaDelObjetoTest`, concurrencia | Cumple |
| ReservaFactory | `domain/model/ReservaFactory` | Reserva confirmada con snapshot inmutable | solo dominio/Java | `ReservaFactoryTest` | Cumple |
| Identidad Client | `IdentidadRestAdapter` | UID, elegibilidad y teléfono por contratos externos | puertos, HTTP configurado | `IntegrationConfigurationTest` | Preparado; contrato externo pendiente |
| Catálogo Client | `CatalogoRestAdapter` | Snapshot autoritativo, ID/hash y disponibilidad | puerto, HTTP configurado | tests de aplicación/configuración | Preparado; contrato externo pendiente |
| Solicitud Repository | `JpaReservationAdapter` + `SolicitudJpaRepository` | mapeo dominio/JPA, locks y deduplicación | infraestructura JPA | `MySqlConcurrencyIT` | Cumple |
| Reserva Repository | `JpaReservationAdapter` + `ReservaJpaRepository` | mapeo, solapamientos y ocupación | infraestructura JPA | `MySqlConcurrencyIT`, `HistoricalMigrationIT` | Cumple |
| Outbox Event Publisher | `OutboxJpaAdapter`, `OutboxRelay` | persistencia transaccional y entrega al menos una vez | JPA y AMQP | `RabbitOutboxIT` | Cumple |

`HexagonalArchitectureTest` impide que `domain` dependa de Spring/JPA/infraestructura y que `application` dependa de infraestructura/interfaces. Las entidades JPA no atraviesan los puertos.
