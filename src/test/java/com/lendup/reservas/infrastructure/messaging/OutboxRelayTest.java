package com.lendup.reservas.infrastructure.messaging;

import static org.junit.jupiter.api.Assertions.*;

import com.lendup.reservas.infrastructure.persistence.entity.OutboxEventEntity;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.AmqpConnectException;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import tools.jackson.databind.json.JsonMapper;

class OutboxRelayTest {
  @Test void conservaPendienteElEventoSiElBrokerEstaCaido() {
    OutboxEventEntity event=new OutboxEventEntity(UUID.randomUUID(),"RESERVA",UUID.randomUUID(),"ReservaConfirmada",1,1,"{}","corr",Instant.now());
    InMemoryStore store=new InMemoryStore(event);
    RabbitTemplate unavailable=new RabbitTemplate(){
      @Override public void convertAndSend(String exchange,String routingKey,Object message,MessagePostProcessor postProcessor,CorrelationData correlationData){
        throw new AmqpConnectException(new java.net.ConnectException("broker caído"));
      }
    };
    new OutboxRelay(store,unavailable,JsonMapper.builder().findAndAddModules().build(),Clock.systemUTC(),10).relay();
    assertAll(
        ()->assertNull(event.getPublishedAt()),
        ()->assertEquals(1,event.getAttempts()),
        ()->assertNotNull(event.getLastError()),
        ()->assertSame(event,store.saved));
  }

  @Test void tipoDesconocidoSeAgotaSinPublicarse() {
    OutboxEventEntity event=new OutboxEventEntity(UUID.randomUUID(),"RESERVA",UUID.randomUUID(),"Desconocido",1,1,"{}","corr",Instant.now());
    InMemoryStore store=new InMemoryStore(event);
    new OutboxRelay(store,new RabbitTemplate(),JsonMapper.builder().findAndAddModules().build(),Clock.systemUTC(),1).relay();
    assertNull(event.getPublishedAt());
    assertEquals(1,event.getAttempts());
  }

  @Test void publicaEnUnIntentoPosteriorCuandoElBrokerSeRecupera() {
    OutboxEventEntity event=new OutboxEventEntity(UUID.randomUUID(),"RESERVA",UUID.randomUUID(),"ReservaConfirmada",1,1,"{}","corr",Instant.now());
    InMemoryStore store=new InMemoryStore(event);
    class RecoveringRabbit extends RabbitTemplate {
      int calls;
      @Override public void convertAndSend(String exchange,String routingKey,Object message,MessagePostProcessor postProcessor,CorrelationData correlationData){
        if(calls++==0) throw new AmqpConnectException(new java.net.ConnectException("broker caído"));
        correlationData.getFuture().complete(new CorrelationData.Confirm(true,null));
      }
    }
    var relay=new OutboxRelay(store,new RecoveringRabbit(),JsonMapper.builder().findAndAddModules().build(),Clock.systemUTC(),10);
    relay.relay();
    assertNull(event.getPublishedAt());
    relay.relay();
    assertNotNull(event.getPublishedAt());
    assertEquals(1,event.getAttempts());
  }

  private static final class InMemoryStore implements OutboxBatchStore {
    private final OutboxEventEntity event;
    private OutboxEventEntity saved;
    private InMemoryStore(OutboxEventEntity event){this.event=event;}
    @Override public List<OutboxEventEntity> lockPending(int limit,int maxAttempts){return event.getAttempts()<maxAttempts?List.of(event):List.of();}
    @Override public void save(OutboxEventEntity event){saved=event;}
  }
}
