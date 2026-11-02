package com.lendup.reservas.integration;

import static org.junit.jupiter.api.Assertions.*;

import com.lendup.reservas.application.port.out.OutboxPort;
import com.lendup.reservas.config.RabbitConfiguration;
import com.lendup.reservas.domain.event.EventEnvelope;
import com.lendup.reservas.infrastructure.messaging.OutboxRelay;
import com.lendup.reservas.infrastructure.persistence.repository.OutboxJpaRepository;
import com.lendup.reservas.support.TestAuthConfiguration;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.*;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.core.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.*;
import org.testcontainers.junit.jupiter.*;
import org.testcontainers.mysql.MySQLContainer;
import org.testcontainers.rabbitmq.RabbitMQContainer;

@Testcontainers(disabledWithoutDocker=true)
@ActiveProfiles("test")
@Import(TestAuthConfiguration.class)
@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.MOCK,properties={"spring.rabbitmq.listener.simple.auto-startup=false","lendup.outbox.relay-delay-ms=3600000"})
class RabbitOutboxIT {
  @Container static final MySQLContainer mysql=new MySQLContainer("mysql:8.4").withDatabaseName("lendup_reservas").withUsername("test").withPassword("test");
  @Container static final RabbitMQContainer rabbit=new RabbitMQContainer("rabbitmq:4.1-management");
  @DynamicPropertySource static void properties(DynamicPropertyRegistry r){r.add("spring.datasource.url",mysql::getJdbcUrl);r.add("spring.datasource.username",mysql::getUsername);r.add("spring.datasource.password",mysql::getPassword);r.add("spring.rabbitmq.host",rabbit::getHost);r.add("spring.rabbitmq.port",rabbit::getAmqpPort);r.add("spring.rabbitmq.username",rabbit::getAdminUsername);r.add("spring.rabbitmq.password",rabbit::getAdminPassword);}
  @Autowired OutboxPort outbox; @Autowired OutboxRelay relay; @Autowired OutboxJpaRepository repository; @Autowired RabbitAdmin admin; @Autowired RabbitTemplate template;
  String queue;
  @BeforeEach void setup(){repository.deleteAll();admin.purgeQueue(RabbitConfiguration.LOAN_QUEUE,true);admin.purgeQueue(RabbitConfiguration.DLQ,true);queue="outbox-capture-"+UUID.randomUUID();admin.declareQueue(new org.springframework.amqp.core.Queue(queue,false,false,false));admin.declareBinding(new Binding(queue,Binding.DestinationType.QUEUE,RabbitConfiguration.EVENTS_EXCHANGE,"reservas.confirmada.v1",null));}
  @AfterEach void cleanup(){admin.deleteQueue(queue);}
  @Test void publicaEventualmenteYMarcaOutboxSoloTrasConfirmacion(){UUID eventId=UUID.randomUUID(),aggregateId=UUID.randomUUID();outbox.append(new EventEnvelope(eventId,"ReservaConfirmada",1,1,aggregateId,Instant.now(),"corr",Map.of("reserva_id",aggregateId)));relay.relay();Message received=template.receive(queue,5000);assertNotNull(received);assertEquals(eventId.toString(),received.getMessageProperties().getMessageId());var stored=repository.findById(eventId).orElseThrow();assertNotNull(stored.getPublishedAt());assertEquals(0,stored.getAttempts());}
  @Test void eventoSinRutaPermanecePendienteConError(){UUID eventId=UUID.randomUUID(),aggregateId=UUID.randomUUID();outbox.append(new EventEnvelope(eventId,"SolicitudCreada",1,1,aggregateId,Instant.now(),"corr",Map.of("solicitud_id",aggregateId)));relay.relay();var stored=repository.findById(eventId).orElseThrow();assertNull(stored.getPublishedAt());assertEquals(1,stored.getAttempts());assertNotNull(stored.getLastError());}
  @Test void dosRelaysNoProcesanSimultaneamenteLaMismaFila()throws Exception{UUID eventId=UUID.randomUUID(),aggregateId=UUID.randomUUID();outbox.append(new EventEnvelope(eventId,"ReservaConfirmada",1,1,aggregateId,Instant.now(),"corr",Map.of("reserva_id",aggregateId)));ExecutorService pool=Executors.newFixedThreadPool(2);try{Future<?> first=pool.submit(relay::relay);Future<?> second=pool.submit(relay::relay);first.get(10,TimeUnit.SECONDS);second.get(10,TimeUnit.SECONDS);assertNotNull(template.receive(queue,5000));assertNull(template.receive(queue,500));assertNotNull(repository.findById(eventId).orElseThrow().getPublishedAt());}finally{pool.shutdownNow();}}
  @Test void rabbitRedeliverYDeadLetterConservanElMensaje()throws Exception{byte[] body="{\"invalid\":true}".getBytes(java.nio.charset.StandardCharsets.UTF_8);template.convertAndSend("",RabbitConfiguration.LOAN_QUEUE,body);Boolean redelivered=template.execute(channel->{var first=channel.basicGet(RabbitConfiguration.LOAN_QUEUE,false);assertNotNull(first);channel.basicReject(first.getEnvelope().getDeliveryTag(),true);var second=channel.basicGet(RabbitConfiguration.LOAN_QUEUE,false);assertNotNull(second);boolean flag=second.getEnvelope().isRedeliver();channel.basicReject(second.getEnvelope().getDeliveryTag(),false);return flag;});assertEquals(Boolean.TRUE,redelivered);Message dead=template.receive(RabbitConfiguration.DLQ,5000);assertNotNull(dead);assertArrayEquals(body,dead.getBody());}
}
