package com.lendup.reservas.infrastructure.messaging;

import com.lendup.reservas.config.RabbitConfiguration;
import com.lendup.reservas.infrastructure.persistence.entity.OutboxEventEntity;
import java.time.Clock;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

@Component
public class OutboxRelay {
  private static final Logger log = LoggerFactory.getLogger(OutboxRelay.class);
  private static final int BATCH_SIZE = 50;
  private final OutboxBatchStore store;
  private final RabbitTemplate rabbit;
  private final JsonMapper json;
  private final Clock clock;
  private final int maxAttempts;

  public OutboxRelay(OutboxBatchStore store, RabbitTemplate rabbit, JsonMapper json, Clock clock,
      @Value("${lendup.outbox.max-attempts:10}") int maxAttempts) {
    this.store = store;
    this.rabbit = rabbit;
    this.json = json;
    this.clock = clock;
    this.maxAttempts = maxAttempts;
  }

  @Scheduled(fixedDelayString = "${lendup.outbox.relay-delay-ms:1000}")
  @Transactional
  public void relay() {
    for (OutboxEventEntity event : store.lockPending(BATCH_SIZE, maxAttempts)) {
      try {
        publish(event);
        event.published(clock.instant());
      } catch (Exception exception) {
        event.failed(exception.getMessage());
        if (event.getAttempts() >= maxAttempts) {
          log.error("outbox_event_exhausted event_id={} event_type={} attempts={} error_type={}",
              event.getId(), event.getEventType(), event.getAttempts(), exception.getClass().getSimpleName());
        } else {
          log.warn("outbox_publish_failed event_id={} event_type={} attempts={} error_type={}",
              event.getId(), event.getEventType(), event.getAttempts(), exception.getClass().getSimpleName());
        }
      }
      store.save(event);
    }
  }

  private void publish(OutboxEventEntity event) throws Exception {
    Map<String, Object> envelope = new LinkedHashMap<>();
    envelope.put("event_id", event.getId());
    envelope.put("event_type", event.getEventType());
    envelope.put("event_version", event.getEventVersion());
    envelope.put("aggregate_version", event.getAggregateVersion());
    envelope.put("aggregate_id", event.getAggregateId());
    envelope.put("occurred_at", event.getOccurredAt());
    envelope.put("correlation_id", event.getCorrelationId());
    envelope.put("payload", json.readValue(event.getPayload(), Map.class));
    CorrelationData correlation = new CorrelationData(event.getId().toString());
    rabbit.convertAndSend(
        RabbitConfiguration.EVENTS_EXCHANGE,
        routingKey(event.getEventType()),
        json.writeValueAsBytes(envelope),
        message -> {
          message.getMessageProperties().setContentType("application/json");
          message.getMessageProperties().setMessageId(event.getId().toString());
          message.getMessageProperties().setHeader("x-event-type", event.getEventType());
          return message;
        }, correlation);
    CorrelationData.Confirm confirm = correlation.getFuture().get(5, TimeUnit.SECONDS);
    if (!confirm.ack()) throw new IllegalStateException("El broker rechazó el evento");
    if (correlation.getReturned() != null) throw new IllegalStateException("El evento no tiene una ruta de entrega");
  }

  private String routingKey(String type) {
    return switch (type) {
      case "SolicitudCreada" -> "reservas.solicitud-creada.v1";
      case "ReservaConfirmada" -> "reservas.confirmada.v1";
      case "CancelacionReservaSolicitada" -> "reservas.cancelacion-solicitada.v1";
      case "ReservaCancelada" -> "reservas.cancelada.v1";
      default -> throw new IllegalArgumentException("Tipo de evento Outbox no soportado");
    };
  }
}
