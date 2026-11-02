package com.lendup.reservas.infrastructure.messaging;

import com.lendup.reservas.application.exception.NotFoundException;
import com.lendup.reservas.application.port.out.*;
import com.lendup.reservas.config.RabbitConfiguration;
import com.lendup.reservas.domain.event.EventEnvelope;
import com.lendup.reservas.infrastructure.persistence.entity.ReservaEntity;
import com.lendup.reservas.infrastructure.persistence.repository.ProcessedEventJpaRepository;
import java.time.*;
import java.util.*;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

@Component
public class LoanEventConsumer {
  private static final String CONSUMER = "reservations.loan-events.v1";
  private static final Set<String> SUPPORTED = Set.of("PrestamoCancelado", "CancelacionRechazada", "PeriodoPrestamoLiberado");
  private final JsonMapper json;
  private final ReservationPersistencePort persistence;
  private final ProcessedEventJpaRepository processed;
  private final OutboxPort outbox;
  private final AuditPort audit;
  private final Clock clock;

  public LoanEventConsumer(JsonMapper json, ReservationPersistencePort persistence, ProcessedEventJpaRepository processed,
      OutboxPort outbox, AuditPort audit, Clock clock) {
    this.json=json; this.persistence=persistence; this.processed=processed; this.outbox=outbox; this.audit=audit; this.clock=clock;
  }

  @RabbitListener(queues = RabbitConfiguration.LOAN_QUEUE)
  @Transactional
  public void consume(byte[] body) throws Exception {
    Map<String,Object> event=json.readValue(body,new TypeReference<Map<String,Object>>(){});
    UUID eventId=uuid(event,"event_id");
    String type=requiredText(event,"event_type");
    if(!SUPPORTED.contains(type)) throw new IllegalArgumentException("Tipo de evento no soportado");
    int eventVersion=number(event,"event_version").intValue();
    if(eventVersion!=1) throw new IllegalArgumentException("Versión de evento no soportada");
    long aggregateVersion=number(event,"aggregate_version").longValue();
    if(aggregateVersion<1) throw new IllegalArgumentException("aggregate_version debe ser positivo");
    Object rawPayload=event.get("payload");
    if(!(rawPayload instanceof Map<?,?> rawMap)) throw new IllegalArgumentException("Payload inválido");
    Map<String,Object> payload=new LinkedHashMap<>();
    rawMap.forEach((key,value)->payload.put(String.valueOf(key),value));
    UUID reservationId=uuid(payload,"reserva_id");
    LocalDateTime releasedUntil=null;
    if(type.equals("PeriodoPrestamoLiberado")) releasedUntil=LocalDateTime.parse(requiredText(payload,"nuevo_hasta"));
    Instant now=clock.instant();
    if(processed.tryRegister(eventId.toString(),CONSUMER,reservationId.toString(),aggregateVersion,now)==0) return;
    ReservaEntity reservation=persistence.lockReserva(reservationId).orElseThrow(()->new NotFoundException("Reserva de evento no encontrada"));
    String previous=reservation.getEstado().name();
    LocalDateTime finalReleasedUntil=releasedUntil;
    boolean changed=switch(type){
      case "PrestamoCancelado"->reservation.aplicarPrestamoCancelado(aggregateVersion,now);
      case "CancelacionRechazada"->reservation.aplicarCancelacionRechazada(aggregateVersion,now);
      case "PeriodoPrestamoLiberado"->reservation.aplicarLiberacion(aggregateVersion,finalReleasedUntil,now);
      default->false;
    };
    if(changed){
      persistence.saveReserva(reservation);
      String correlation=String.valueOf(event.getOrDefault("correlation_id",""));
      audit.record("RESERVA",reservationId,"APLICAR_"+type,previous,reservation.getEstado().name(),null,correlation);
      if(type.equals("PrestamoCancelado")) outbox.append(new EventEnvelope(UUID.randomUUID(),"ReservaCancelada",1,reservation.getAggregateVersion(),reservationId,now,correlation,Map.of("reserva_id",reservationId,"solicitud_id",reservation.getSolicitud().getId())));
    }
  }

  private static UUID uuid(Map<String,Object> values,String key){return UUID.fromString(requiredText(values,key));}
  private static String requiredText(Map<String,Object> values,String key){Object value=values.get(key);if(value==null||value.toString().isBlank())throw new IllegalArgumentException("Campo obligatorio ausente: "+key);return value.toString();}
  private static Number number(Map<String,Object> values,String key){Object value=values.get(key);if(!(value instanceof Number number))throw new IllegalArgumentException("Campo numérico inválido: "+key);return number;}
}
