package com.lendup.reservas.application.service;

import static com.lendup.reservas.domain.specification.SolicitudSpecifications.EstadoComprobacion.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.lendup.reservas.application.exception.*;
import com.lendup.reservas.application.port.in.ReservationModels.CrearSolicitudCommand;
import com.lendup.reservas.application.port.out.*;
import com.lendup.reservas.domain.model.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ReservationApplicationServiceTest {
  ReservationPersistencePort persistence = mock(ReservationPersistencePort.class);
  CatalogoPort catalog = mock(CatalogoPort.class);
  IdentidadPort identity = mock(IdentidadPort.class);
  SolicitudEligibilityPort eligibility = mock(SolicitudEligibilityPort.class);
  PrestamoAccessPort loan = mock(PrestamoAccessPort.class);
  OutboxPort outbox = mock(OutboxPort.class);
  AuditPort audit = mock(AuditPort.class);
  Clock clock = Clock.fixed(Instant.parse("2026-11-01T12:00:00Z"), ZoneOffset.UTC);
  ReservationApplicationService service = new ReservationApplicationService(persistence,
      catalog, identity, eligibility, loan, outbox, audit, clock, new ReservaFactory(),
      () -> "test-correlation");
  UUID actor = UUID.randomUUID(), publication = UUID.randomUUID(), owner = UUID.randomUUID();

  @BeforeEach void setup() {
    when(catalog.obtenerSnapshot(any(), any())).thenReturn(new CatalogoPort.PublicacionSnapshot(
        publication, owner, true, true, "v1", HASH, new CondicionesAcordadas(
            new BigDecimal("10.00"), null, "PEN", "Campus", "uso", "entrega",
            "devolucion", "cancelacion")));
    when(persistence.saveSolicitud(any())).thenAnswer(invocation -> invocation.getArgument(0));
  }

  @Test void evaluaVerificacionInstitucionalYTerminosEnElCasoDeUso() {
    when(eligibility.obtain(actor)).thenReturn(
        new SolicitudEligibilityPort.EligibilityFacts(NO_CUMPLE, CUMPLE));
    assertThrows(ForbiddenException.class, () -> service.crearSolicitud(command(true, HASH)));
    when(eligibility.obtain(actor)).thenReturn(
        new SolicitudEligibilityPort.EligibilityFacts(CUMPLE, NO_CUMPLE));
    assertThrows(ForbiddenException.class, () -> service.crearSolicitud(command(true, HASH)));
    verify(persistence, never()).saveSolicitud(any());
  }

  @Test void desconocidoFallaSeguroComoDependenciaNoDisponible() {
    when(eligibility.obtain(actor)).thenReturn(
        new SolicitudEligibilityPort.EligibilityFacts(DESCONOCIDO, CUMPLE));
    assertThrows(DependencyUnavailableException.class,
        () -> service.crearSolicitud(command(true, HASH)));
  }

  @Test void correoFirebaseNoSustituyeVerificacionInstitucional() {
    when(eligibility.obtain(actor)).thenReturn(
        new SolicitudEligibilityPort.EligibilityFacts(CUMPLE, CUMPLE));
    assertThrows(ForbiddenException.class, () -> service.crearSolicitud(command(false, HASH)));
  }

  @Test void snapshotObsoletoOAlteradoSeRechaza() {
    when(eligibility.obtain(actor)).thenReturn(
        new SolicitudEligibilityPort.EligibilityFacts(CUMPLE, CUMPLE));
    assertThrows(BusinessConflictException.class,
        () -> service.crearSolicitud(command(true, "b".repeat(64))));
  }

  @Test void solicitudValidaRegistraAceptacionConHoraDelServidor() {
    when(eligibility.obtain(actor)).thenReturn(
        new SolicitudEligibilityPort.EligibilityFacts(CUMPLE, CUMPLE));
    var created = service.crearSolicitud(command(true, HASH));
    assertAll(
        () -> assertEquals("v1", created.condicionesSnapshotId()),
        () -> assertEquals(HASH, created.condicionesSnapshotHash()),
        () -> assertEquals(clock.instant(), created.condicionesAceptadasEn()));
    verify(outbox).append(any());
  }

  private CrearSolicitudCommand command(boolean emailVerified, String hash) {
    return new CrearSolicitudCommand(actor, emailVerified, publication,
        LocalDateTime.of(2026, 11, 10, 9, 0), LocalDateTime.of(2026, 11, 11, 9, 0),
        "v1", hash, true);
  }

  private static final String HASH = "a".repeat(64);
}
