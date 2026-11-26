package com.lendup.reservas.infrastructure.messaging;

import com.lendup.reservas.application.port.out.OutboxPort;
import com.lendup.reservas.application.port.out.ReservationPersistencePort;
import com.lendup.reservas.domain.event.EventEnvelope;
import com.lendup.reservas.domain.model.*;
import com.lendup.reservas.infrastructure.persistence.ExtensionHoldStore;
import com.lendup.reservas.infrastructure.persistence.ExtensionHoldStore.Hold;
import com.lendup.reservas.infrastructure.persistence.ExtensionHoldStore.Status;
import java.time.*;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/** La fila de agenda serializa altas, confirmaciones y expiraciones de la misma publicación. */
@Component
public class ExtensionHoldWorkflow {
  public record Request(UUID change, UUID publication, UUID reservation, UUID loan,
                        LocalDateTime from, LocalDateTime to) {}

  private final ReservationPersistencePort reservations;
  private final ExtensionHoldStore holds;
  private final OutboxPort outbox;
  private final Clock clock;
  private final Duration ttl;
  private final TransactionTemplate expiration;

  public ExtensionHoldWorkflow(ReservationPersistencePort reservations, ExtensionHoldStore holds, OutboxPort outbox,
      Clock clock, PlatformTransactionManager transactions, @Value("${lendup.extensions.hold-seconds:900}") long holdSeconds) {
    if (holdSeconds <= 0) throw new IllegalArgumentException("La retención debe tener duración positiva");
    this.reservations = reservations;
    this.holds = holds;
    this.outbox = outbox;
    this.clock = clock;
    this.ttl = Duration.ofSeconds(holdSeconds);
    this.expiration = new TransactionTemplate(transactions);
    expiration.setIsolationLevel(TransactionDefinition.ISOLATION_READ_COMMITTED);
  }

  public void handle(String type, Request request, long loanVersion, String correlation) {
    var interval = new PeriodoPrestamo(request.from(), request.to());
    var agenda = reservations.lockAgenda(request.publication());
    var reservation = reservations.lockReserva(request.reservation()).orElseThrow();
    if (!agenda.id().equals(reservation.agendaId())) throw new IllegalArgumentException("Reserva fuera de su agenda");
    var actual = holds.find(request.change()).orElse(null);
    if (actual != null && (!actual.publicationId().equals(request.publication())
        || !actual.reservationId().equals(request.reservation()) || !actual.loanId().equals(request.loan())
        || !actual.startsAt().equals(request.from()) || !actual.endsAt().equals(request.to()))) {
      throw new IllegalArgumentException("La operación de extensión no coincide con la retención registrada");
    }
    Instant now = clock.instant();
    if (type.equals("ExtensionRetencionSolicitada")) {
      if (actual != null) return;
      boolean disponible = apta(reservation, request) && agenda.estaDisponible(interval);
      var hold = new Hold(request.change(), request.publication(), request.reservation(), request.loan(),
          request.from(), request.to(), now.plus(ttl), disponible ? Status.RETENIDA : Status.RECHAZADA);
      holds.insert(hold);
      if (disponible) publish("ExtensionRetenida", hold, "RESERVADA", 1, correlation, now);
      else publish("ExtensionRetencionRechazada", hold, "SIN_DISPONIBILIDAD", 1, correlation, now);
      return;
    }
    if (actual == null) {
      if (!type.equals("ExtensionRetencionCancelada")) throw new IllegalStateException("Retención aún no disponible");
      actual = new Hold(request.change(), request.publication(), request.reservation(), request.loan(),
          request.from(), request.to(), now, Status.RECHAZADA);
      holds.insert(actual);
      publish("ExtensionRetencionRechazada", actual, "CANCELADA", 1, correlation, now);
      return;
    }
    if (actual.status() != Status.RETENIDA) return;
    if (type.equals("ExtensionRetencionCancelada")) {
      reject(actual, "CANCELADA", correlation, now);
    } else if (!now.isBefore(actual.expiresAt())) {
      reject(actual, "EXPIRADA", correlation, now);
    } else if (!apta(reservation, request) || agenda.bloqueos().stream()
        .anyMatch(b -> !b.reservaId().equals(request.change()) && b.mantienePeriodoBloqueado()
            && b.periodo().seSuperponeCon(interval))
        || !reservation.aplicarActualizacionPeriodo(loanVersion, request.to(), now)) {
      reject(actual, "OCUPACION_INCOMPATIBLE", correlation, now);
    } else {
      reservations.saveReserva(reservation, reservations.findSolicitudForReserva(reservation.id()).orElseThrow());
      holds.finish(actual.changeId(), Status.CONFIRMADA);
      publish("ExtensionOcupacionConfirmada", actual, "CONFIRMADA", 2, correlation, now);
    }
  }

  @Scheduled(fixedDelayString = "${lendup.extensions.expiration-delay-ms:1000}")
  void expire() {
    for (var candidate : holds.expired(clock.instant())) {
      expiration.executeWithoutResult(tx -> {
        reservations.lockAgenda(candidate.publicationId());
        var current = holds.find(candidate.changeId()).orElseThrow();
        var now = clock.instant();
        if (current.status() == Status.RETENIDA && !now.isBefore(current.expiresAt())) {
          reject(current, "EXPIRADA", current.changeId().toString(), now);
        }
      });
    }
  }

  private static boolean apta(Reserva reservation, Request request) {
    return reservation.estado() == EstadoReserva.CONFIRMADA && reservation.ocupacionHasta().equals(request.from())
        && reservation.periodoLiberadoDesde() == null;
  }

  private void reject(Hold hold, String cause, String correlation, Instant now) {
    holds.finish(hold.changeId(), Status.RECHAZADA);
    publish("ExtensionRetencionRechazada", hold, cause, 2, correlation, now);
  }

  private void publish(String type, Hold hold, String reason, long version, String correlation, Instant now) {
    outbox.append(new EventEnvelope(UUID.randomUUID(), type, 1, version, hold.changeId(), now, correlation,
        Map.of("reserva_id", hold.reservationId(), "prestamo_id", hold.loanId(), "publicacion_id", hold.publicationId(),
            "cambio_fecha_prestamo_id", hold.changeId(), "expira_en", hold.expiresAt().toString(), "motivo", reason)));
  }
}
