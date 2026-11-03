package com.lendup.reservas.integration;

import static org.junit.jupiter.api.Assertions.*;

import com.lendup.reservas.application.exception.*;
import com.lendup.reservas.application.port.in.ReservationModels.CrearSolicitudCommand;
import com.lendup.reservas.application.port.out.*;
import com.lendup.reservas.application.service.ReservationApplicationService;
import com.lendup.reservas.domain.model.*;
import com.lendup.reservas.domain.specification.SolicitudSpecifications.EstadoComprobacion;
import com.lendup.reservas.infrastructure.messaging.LoanEventConsumer;
import com.lendup.reservas.infrastructure.persistence.entity.SolicitudEntity;
import com.lendup.reservas.infrastructure.persistence.repository.*;
import com.lendup.reservas.support.TestAuthConfiguration;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.*;
import org.springframework.context.annotation.*;
import org.springframework.test.context.*;
import org.testcontainers.junit.jupiter.*;
import org.testcontainers.mysql.MySQLContainer;
import tools.jackson.databind.json.JsonMapper;

@Testcontainers(disabledWithoutDocker = true)
@ActiveProfiles("test")
@Import({TestAuthConfiguration.class, MySqlConcurrencyIT.StubConfiguration.class})
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK, properties = {
    "spring.rabbitmq.listener.simple.auto-startup=false",
    "lendup.outbox.relay-delay-ms=3600000",
    "spring.datasource.hikari.maximum-pool-size=25",
    "springdoc.api-docs.enabled=true",
    "springdoc.swagger-ui.enabled=true"})
class MySqlConcurrencyIT {
  @Container static final MySQLContainer mysql = new MySQLContainer("mysql:8.4")
      .withDatabaseName("lendup_reservas").withUsername("test").withPassword("test");

  @DynamicPropertySource
  static void db(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", mysql::getJdbcUrl);
    registry.add("spring.datasource.username", mysql::getUsername);
    registry.add("spring.datasource.password", mysql::getPassword);
  }

  @Autowired ReservationApplicationService service;
  @Autowired SolicitudJpaRepository solicitudes;
  @Autowired ReservaJpaRepository reservas;
  @Autowired AgendaJpaRepository agendas;
  @Autowired OutboxJpaRepository outbox;
  @Autowired AuditJpaRepository audits;
  @Autowired ProcessedEventJpaRepository processed;
  @Autowired LoanEventConsumer consumer;
  @Autowired JsonMapper json;
  @Autowired org.springframework.transaction.PlatformTransactionManager transactionManager;
  @Autowired org.springframework.web.context.WebApplicationContext webContext;
  @Autowired TestCatalogo catalogo;
  @Autowired TestPrestamoAccess prestamoAccess;

  UUID lender;
  UUID publication;

  @BeforeEach
  void setup() {
    processed.deleteAll();
    outbox.deleteAll();
    audits.deleteAll();
    reservas.deleteAll();
    solicitudes.deleteAll();
    agendas.deleteAll();
    lender = UUID.randomUUID();
    publication = UUID.randomUUID();
    catalogo.owner.set(lender);
    catalogo.conditions.set(terms("10.00", "uso inicial"));
    prestamoAccess.allowed = true;
  }

  @Test void veinteAceptacionesSuperpuestasConfirmanExactamenteUna() throws Exception {
    List<UUID> ids = new ArrayList<>();
    for (int i = 0; i < 20; i++) ids.add(pending(UUID.randomUUID(), FROM, UNTIL));
    ExecutorService pool = Executors.newFixedThreadPool(20);
    try {
      List<Future<Boolean>> futures = ids.stream()
          .map(id -> pool.submit(() -> attempt(id))).toList();
      int accepted = 0;
      for (Future<Boolean> future : futures) if (future.get(30, TimeUnit.SECONDS)) accepted++;
      assertEquals(1, accepted);
      assertEquals(1, reservas.count());
      assertEquals(1, agendas.count());
    } finally {
      pool.shutdownNow();
    }
  }

  @Test void dosSolicitudesPendientesIdenticasConcurrentesCreanSoloUna() throws Exception {
    UUID borrower = UUID.randomUUID();
    CrearSolicitudCommand command = command(borrower, FROM, UNTIL);
    ExecutorService pool = Executors.newFixedThreadPool(2);
    try {
      Future<Boolean> first = pool.submit(() -> createAttempt(command));
      Future<Boolean> second = pool.submit(() -> createAttempt(command));
      assertEquals(1, (first.get(15, TimeUnit.SECONDS) ? 1 : 0)
          + (second.get(15, TimeUnit.SECONDS) ? 1 : 0));
      assertEquals(1, solicitudes.count());
    } finally {
      pool.shutdownNow();
    }
  }

  @Test void periodosConsecutivosPuedenConfirmarse() {
    UUID first = pending(UUID.randomUUID(), FROM, UNTIL);
    UUID second = pending(UUID.randomUUID(), UNTIL, UNTIL.plusDays(1));
    service.aceptarSolicitud(lender, first);
    service.aceptarSolicitud(lender, second);
    assertEquals(2, reservas.count());
  }

  @Test void mismaSolicitudAceptadaDosVecesCreaUnaReserva() throws Exception {
    UUID id = pending(UUID.randomUUID(), FROM, UNTIL);
    ExecutorService pool = Executors.newFixedThreadPool(2);
    try {
      Future<Boolean> first = pool.submit(() -> attempt(id));
      Future<Boolean> second = pool.submit(() -> attempt(id));
      assertEquals(1, (first.get() ? 1 : 0) + (second.get() ? 1 : 0));
      assertEquals(1, reservas.count());
    } finally {
      pool.shutdownNow();
    }
  }

  @Test void cancelacionPendienteMantieneElPeriodoBloqueado() {
    UUID first = pending(UUID.randomUUID(), FROM, UNTIL);
    UUID reservation = service.aceptarSolicitud(lender, first).id();
    service.solicitarCancelacionReserva(lender, false, reservation, "Cambio", "corr");
    UUID second = pending(UUID.randomUUID(), FROM.plusHours(1), UNTIL.plusHours(1));
    assertThrows(BusinessConflictException.class,
        () -> service.aceptarSolicitud(lender, second));
  }

  @Test void reservaYOutboxSeReviertenJuntasSiFallaLaTransaccion() {
    UUID id = pending(UUID.randomUUID(), FROM, UNTIL);
    var transaction = new org.springframework.transaction.support.TransactionTemplate(transactionManager);
    assertThrows(RuntimeException.class, () -> transaction.executeWithoutResult(ignored -> {
      service.aceptarSolicitud(lender, id);
      throw new RuntimeException("fallo forzado");
    }));
    assertEquals(0, reservas.count());
    assertEquals(0, outbox.count());
    assertEquals(EstadoSolicitud.PENDIENTE,
        solicitudes.findById(id).orElseThrow().getEstado());
  }

  @Test void snapshotHistoricoNoCambiaCuandoCatalogoSeActualiza() {
    UUID borrower = UUID.randomUUID();
    var created = service.crearSolicitud(command(borrower, FROM, UNTIL));
    catalogo.conditions.set(terms("99.00", "uso modificado"));
    var reservation = service.aceptarSolicitud(lender, created.id());
    assertAll(
        () -> assertEquals(new BigDecimal("10.00"), reservation.tarifaDiariaAcordada()),
        () -> assertEquals("uso inicial", reservation.condicionesUsoAcordadas()),
        () -> assertEquals(SNAPSHOT_HASH, reservation.condicionesSnapshotHash()));
  }

  @Test void liberacionAutoritativaConservaHastaContractual() throws Exception {
    UUID request = pending(UUID.randomUUID(), FROM, UNTIL);
    UUID reservationId = service.aceptarSolicitud(lender, request).id();
    consumer.consume(loanEvent(UUID.randomUUID(), "PeriodoPrestamoLiberado", 1,
        reservationId, Map.of("nuevo_hasta", UNTIL.minusHours(2).toString(),
            "decision", "LIBERACION_CONFIRMADA",
            "devolucion_confirmada_por_prestamista", true,
            "incidencias_pendientes", false)));
    Reserva stored = reservas.findById(reservationId).orElseThrow().toDomain();
    assertEquals(UNTIL, stored.periodoContractual().hasta());
    assertEquals(UNTIL.minusHours(2), stored.ocupacionHasta());
  }

  @Test void liberacionSinEvidenciaSeRechazaYNoSeRegistra() {
    UUID request = pending(UUID.randomUUID(), FROM, UNTIL);
    UUID reservationId = service.aceptarSolicitud(lender, request).id();
    assertThrows(IllegalArgumentException.class, () -> consumer.consume(
        loanEvent(UUID.randomUUID(), "PeriodoPrestamoLiberado", 1, reservationId,
            Map.of("nuevo_hasta", UNTIL.minusHours(2).toString()))));
    assertEquals(0, processed.count());
    assertEquals(UNTIL, reservas.findById(reservationId).orElseThrow().toDomain().ocupacionHasta());
  }

  @Test void consumidorDeduplicaYOrdenaEventos() throws Exception {
    UUID request = pending(UUID.randomUUID(), FROM, UNTIL);
    UUID reservationId = service.aceptarSolicitud(lender, request).id();
    service.solicitarCancelacionReserva(lender, false, reservationId, "Cambio", "corr");
    UUID eventId = UUID.randomUUID();
    byte[] cancelled = loanEvent(eventId, "PrestamoCancelado", 2, reservationId, Map.of());
    consumer.consume(cancelled);
    consumer.consume(cancelled);
    consumer.consume(loanEvent(UUID.randomUUID(), "CancelacionRechazada", 1,
        reservationId, Map.of()));
    assertEquals(EstadoReserva.CANCELADA,
        reservas.findById(reservationId).orElseThrow().getEstado());
    assertEquals(2, processed.count());
  }

  @Test void contactoSoloParticipanteYConVigenciaConfirmada() {
    UUID borrower = UUID.randomUUID();
    UUID request = pending(borrower, FROM, UNTIL);
    UUID reservationId = service.aceptarSolicitud(lender, request).id();
    assertEquals("999999999", service.obtenerContacto(borrower, false, reservationId).telefono());
    assertThrows(ForbiddenException.class,
        () -> service.obtenerContacto(UUID.randomUUID(), true, reservationId));
    prestamoAccess.allowed = false;
    assertThrows(ForbiddenException.class,
        () -> service.obtenerContacto(borrower, false, reservationId));
  }

  @Test void solicitudesRechazadasYCanceladasPublicanEventosVersionados() {
    UUID rejected = pending(UUID.randomUUID(), FROM, UNTIL);
    service.rechazarSolicitud(lender, rejected, "No disponible");
    UUID borrower = UUID.randomUUID();
    UUID cancelled = pending(borrower, FROM.plusDays(2), UNTIL.plusDays(2));
    service.cancelarSolicitud(borrower, cancelled, "Cambio de planes");
    assertTrue(outbox.findAll().stream().anyMatch(e -> e.getEventType().equals("SolicitudRechazada")));
    assertTrue(outbox.findAll().stream().anyMatch(e -> e.getEventType().equals("SolicitudCancelada")));
  }

  @Test void seguridadNoCreaUsuariosNiHabilitaBasicOFormLogin() throws Exception {
    assertTrue(webContext.getBeansOfType(
        org.springframework.security.core.userdetails.UserDetailsService.class).isEmpty());
    var mvc = mockMvc();
    mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
        .get("/api/v1/reservas").header("Authorization", "Basic dXNlcjpwYXNz"))
        .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isUnauthorized())
        .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.header()
            .doesNotExist("WWW-Authenticate"));
    mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/login"))
        .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isUnauthorized());
  }

  @Test void openApiAutenticadoExponeFirebaseBearerEnNueveOperaciones() throws Exception {
    String body = mockMvc().perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
        .get("/v3/api-docs").with(org.springframework.security.test.web.servlet.request
            .SecurityMockMvcRequestPostProcessors.user("firebase-uid")))
        .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk())
        .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers
            .jsonPath("$.components.securitySchemes.firebaseBearer.scheme").value("bearer"))
        .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers
            .jsonPath("$.security[0].firebaseBearer").isArray())
        .andReturn().getResponse().getContentAsString();
    assertEquals(9, body.split("\"operationId\"", -1).length - 1);
  }

  private org.springframework.test.web.servlet.MockMvc mockMvc() {
    return org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup(webContext)
        .apply(org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity())
        .build();
  }

  private boolean attempt(UUID id) {
    try { service.aceptarSolicitud(lender, id); return true; }
    catch (BusinessConflictException exception) { return false; }
  }

  private boolean createAttempt(CrearSolicitudCommand command) {
    try { service.crearSolicitud(command); return true; }
    catch (BusinessConflictException exception) { return false; }
  }

  private CrearSolicitudCommand command(UUID borrower, LocalDateTime from, LocalDateTime until) {
    return new CrearSolicitudCommand(borrower, true, publication, from, until,
        SNAPSHOT_ID, SNAPSHOT_HASH, true);
  }

  private UUID pending(UUID borrower, LocalDateTime from, LocalDateTime until) {
    Instant now = Instant.now();
    Solicitud request = Solicitud.crear(UUID.randomUUID(), publication, borrower, lender,
        new PeriodoPrestamo(from, until), catalogo.conditions.get(),
        new AceptacionCondiciones(SNAPSHOT_ID, SNAPSHOT_HASH, now), now);
    solicitudes.saveAndFlush(SolicitudEntity.fromDomain(request));
    return request.id();
  }

  private byte[] loanEvent(UUID eventId, String type, long version, UUID reservationId,
      Map<String, Object> additionalPayload) throws Exception {
    Map<String, Object> payload = new LinkedHashMap<>(additionalPayload);
    payload.put("reserva_id", reservationId);
    return json.writeValueAsBytes(Map.of("event_id", eventId, "event_type", type,
        "event_version", 1, "aggregate_id", reservationId, "aggregate_version", version,
        "occurred_at", Instant.now(), "correlation_id", "corr", "payload", payload));
  }

  private CondicionesAcordadas terms(String rate, String usage) {
    return new CondicionesAcordadas(new BigDecimal(rate), null, "PEN", "Campus",
        usage, "entrega", "devolución", "cancelación");
  }

  @TestConfiguration
  static class StubConfiguration {
    @Bean @Primary TestCatalogo testCatalogo() { return new TestCatalogo(); }
    @Bean @Primary SolicitudEligibilityPort eligibility() {
      return ignored -> new SolicitudEligibilityPort.EligibilityFacts(
          EstadoComprobacion.CUMPLE, EstadoComprobacion.CUMPLE);
    }
    @Bean @Primary TestPrestamoAccess testPrestamoAccess() { return new TestPrestamoAccess(); }
    @Bean @Primary IdentidadPort testIdentity() {
      return new IdentidadPort() {
        public UUID resolverUsuarioId(String ignored) { return UUID.randomUUID(); }
        public String obtenerTelefonoAutorizado(UUID ignored, UUID reservation) { return "999999999"; }
      };
    }
  }

  static class TestCatalogo implements CatalogoPort {
    final AtomicReference<UUID> owner = new AtomicReference<>();
    final AtomicReference<CondicionesAcordadas> conditions = new AtomicReference<>();
    public PublicacionSnapshot obtenerSnapshot(UUID id, PeriodoPrestamo period) {
      return new PublicacionSnapshot(id, owner.get(), true, true, SNAPSHOT_ID,
          SNAPSHOT_HASH, conditions.get());
    }
  }

  static class TestPrestamoAccess implements PrestamoAccessPort {
    volatile boolean allowed = true;
    public boolean contactoVigente(UUID reservationId, UUID participantId) { return allowed; }
  }

  private static final LocalDateTime FROM = LocalDateTime.of(2026, 11, 10, 9, 0);
  private static final LocalDateTime UNTIL = LocalDateTime.of(2026, 11, 12, 9, 0);
  private static final String SNAPSHOT_ID = "catalog-v1";
  private static final String SNAPSHOT_HASH = "a".repeat(64);
}
