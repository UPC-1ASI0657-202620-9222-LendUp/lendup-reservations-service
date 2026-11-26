package com.lendup.reservas.infrastructure.messaging;

import com.lendup.reservas.application.exception.NotFoundException;
import com.lendup.reservas.application.port.out.*;
import com.lendup.reservas.config.RabbitConfiguration;
import com.lendup.reservas.domain.event.EventEnvelope;
import com.lendup.reservas.domain.model.Reserva;
import com.lendup.reservas.domain.model.AgendaDelObjeto;
import com.lendup.reservas.domain.model.PeriodoPrestamo;
import com.lendup.reservas.domain.model.Solicitud;
import com.lendup.reservas.infrastructure.persistence.repository.ProcessedEventJpaRepository;
import java.time.*;
import java.util.*;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

@Component
public class LoanEventConsumer {
  private static final String CONSUMER = "reservations.loan-events.v1";
  private static final Set<String> SUPPORTED = Set.of(
      "PrestamoCancelado",
      "CancelacionRechazada",
      "PeriodoPrestamoLiberado",
      "FechaDevolucionActualizada");
  private final JsonMapper json;
  private final ReservationPersistencePort persistence;
  private final ProcessedEventJpaRepository processed;
  private final OutboxPort outbox;
  private final AuditPort audit;
  private final Clock clock;

  public LoanEventConsumer(JsonMapper json, ReservationPersistencePort persistence,
      ProcessedEventJpaRepository processed,
      OutboxPort outbox, AuditPort audit, Clock clock) {
    this.json = json;
    this.persistence = persistence;
    this.processed = processed;
    this.outbox = outbox;
    this.audit = audit;
    this.clock = clock;
  }

  @RabbitListener(queues = RabbitConfiguration.LOAN_QUEUE)
  @Transactional(isolation = Isolation.READ_COMMITTED)
  public void consume(byte[] body) throws Exception {
    Map<String, Object> event = json.readValue(body, new TypeReference<Map<String, Object>>() {
    });
    UUID eventId = uuid(event, "event_id");
    String type = requiredText(event, "event_type");
    if (!SUPPORTED.contains(type))
      throw new IllegalArgumentException("Tipo de evento no soportado");
    int eventVersion = number(event, "event_version").intValue();
    if (eventVersion != 1)
      throw new IllegalArgumentException("Versión de evento no soportada");
    long aggregateVersion = number(event, "aggregate_version").longValue();
    if (aggregateVersion < 1)
      throw new IllegalArgumentException("aggregate_version debe ser positivo");
    Object rawPayload = event.get("payload");
    if (!(rawPayload instanceof Map<?, ?> rawMap))
      throw new IllegalArgumentException("Payload inválido");
    Map<String, Object> payload = new LinkedHashMap<>();
    rawMap.forEach((key, value) -> payload.put(String.valueOf(key), value));
    UUID reservationId = uuid(payload, "reserva_id");
    LocalDateTime releasedUntil = null;
    LocalDateTime updatedUntil = null;

    if (type.equals("PeriodoPrestamoLiberado")) {
      releasedUntil = LocalDateTime.parse(
          requiredText(payload, "nuevo_hasta"));
      requireReleaseProof(payload);
    }

    if (type.equals("FechaDevolucionActualizada")) {
      updatedUntil = LocalDateTime.parse(
          requiredText(payload, "devolucion_vigente_en"));

      LocalDateTime previousUntil = LocalDateTime.parse(
          requiredText(payload, "devolucion_anterior_en"));

      String changeType = requiredText(payload, "tipo");

      uuid(payload, "cambio_fecha_prestamo_id");

      if (!Set.of("EXTENSION", "REPROGRAMACION")
          .contains(changeType)) {
        throw new IllegalArgumentException(
            "Tipo de cambio de fecha inválido");
      }

      if ("EXTENSION".equals(changeType)
          && !updatedUntil.isAfter(previousUntil)) {
        throw new IllegalArgumentException(
            "La extensión debe ampliar la fecha anterior");
      }
    }
    Instant now = clock.instant();
    if (processed.tryRegister(eventId.toString(), CONSUMER, reservationId.toString(), aggregateVersion, now) == 0)
      return;
    AgendaDelObjeto agenda = null;
    UUID publicationId = null;

    if (type.equals("FechaDevolucionActualizada")) {
      publicationId = uuid(payload, "publicacion_id");
      agenda = persistence.lockAgenda(publicationId);
    }

    Reserva reservation = persistence.lockReserva(reservationId)
        .orElseThrow(() -> new NotFoundException(
            "Reserva de evento no encontrada"));

    Solicitud request = persistence
        .findSolicitudForReserva(reservationId)
        .orElseThrow(() -> new NotFoundException(
            "Solicitud de reserva no encontrada"));

    if (publicationId != null
        && !publicationId.equals(request.publicacionId())) {
      throw new IllegalArgumentException(
          "La publicacion no corresponde a la reserva");
    }

    String previous = reservation.estado().name();
    LocalDateTime finalReleasedUntil = releasedUntil;
    LocalDateTime finalUpdatedUntil = updatedUntil;
    AgendaDelObjeto finalAgenda = agenda;
    boolean changed = switch (type) {

      case "PrestamoCancelado" ->
        reservation.aplicarPrestamoCancelado(
            aggregateVersion, now);

      case "CancelacionRechazada" ->
        reservation.aplicarCancelacionRechazada(
            aggregateVersion, now);

      case "PeriodoPrestamoLiberado" ->
        reservation.aplicarLiberacionConfirmada(
            aggregateVersion, finalReleasedUntil, now);

      case "FechaDevolucionActualizada" -> {
        boolean updated = reservation.aplicarActualizacionPeriodo(
            aggregateVersion, finalUpdatedUntil, now);

        if (updated) {
          validarDisponibilidad(finalAgenda, reservation);
        }

        yield updated;
      }

      default -> false;
    };
    if (changed) {
      persistence.saveReserva(reservation, request);
      String correlation = String.valueOf(event.getOrDefault("correlation_id", ""));
      audit.record("RESERVA", reservationId, "APLICAR_" + type, previous, reservation.estado().name(), null,
          correlation);
      if (type.equals("PrestamoCancelado"))
        outbox.append(new EventEnvelope(UUID.randomUUID(), "ReservaCancelada", 1, reservation.aggregateVersion(),
            reservationId, now, correlation, Map.of("reserva_id", reservationId, "solicitud_id", request.id())));
    }
  }

  private void validarDisponibilidad(
      AgendaDelObjeto agenda, Reserva reservation) {

    PeriodoPrestamo nuevoPeriodo = reservation.periodoOcupacion();

    boolean existeConflicto = agenda.bloqueos().stream()
        .filter(bloqueo -> !bloqueo.reservaId().equals(reservation.id()))
        .filter(AgendaDelObjeto.Bloqueo::mantienePeriodoBloqueado)
        .anyMatch(bloqueo -> nuevoPeriodo.seSuperponeCon(bloqueo.periodo()));

    if (existeConflicto) {
      throw new IllegalStateException(
          "La nueva fecha genera una superposición con otra reserva");
    }
  }

  private static UUID uuid(Map<String, Object> values, String key) {
    return UUID.fromString(requiredText(values, key));
  }

  private static String requiredText(Map<String, Object> values, String key) {
    Object value = values.get(key);
    if (value == null || value.toString().isBlank())
      throw new IllegalArgumentException("Campo obligatorio ausente: " + key);
    return value.toString();
  }

  private static Number number(Map<String, Object> values, String key) {
    Object value = values.get(key);
    if (!(value instanceof Number number))
      throw new IllegalArgumentException("Campo numérico inválido: " + key);
    return number;
  }

  private static void requireReleaseProof(Map<String, Object> payload) {
    if (!"LIBERACION_CONFIRMADA".equals(payload.get("decision"))
        || !Boolean.TRUE.equals(payload.get("devolucion_confirmada_por_prestamista"))
        || !Boolean.FALSE.equals(payload.get("incidencias_pendientes"))) {
      throw new IllegalArgumentException("La liberación no contiene evidencia autoritativa suficiente");
    }
  }
}
