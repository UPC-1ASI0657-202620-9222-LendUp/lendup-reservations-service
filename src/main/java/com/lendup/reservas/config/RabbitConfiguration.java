package com.lendup.reservas.config;
import org.springframework.amqp.core.Binding; import org.springframework.amqp.core.BindingBuilder; import org.springframework.amqp.core.DirectExchange; import org.springframework.amqp.core.ExchangeBuilder; import org.springframework.amqp.core.Queue; import org.springframework.amqp.core.QueueBuilder; import org.springframework.amqp.core.TopicExchange; import org.springframework.context.annotation.*;
@Configuration public class RabbitConfiguration {
  public static final String EVENTS_EXCHANGE="lendup.domain.events"; public static final String DLX="lendup.domain.events.dlx"; public static final String LOAN_QUEUE="reservations.loan-events.v1"; public static final String DLQ="reservations.loan-events.v1.dlq";
  @Bean TopicExchange domainExchange(){return ExchangeBuilder.topicExchange(EVENTS_EXCHANGE).durable(true).build();} @Bean DirectExchange deadLetterExchange(){return ExchangeBuilder.directExchange(DLX).durable(true).build();}
  @Bean Queue loanEventsQueue(){return QueueBuilder.durable(LOAN_QUEUE).deadLetterExchange(DLX).deadLetterRoutingKey(DLQ).build();} @Bean Queue loanEventsDlq(){return QueueBuilder.durable(DLQ).build();}
  @Bean Binding loanCancelled(Queue loanEventsQueue,TopicExchange domainExchange){return BindingBuilder.bind(loanEventsQueue).to(domainExchange).with("prestamos.cancelado.v1");}
  @Bean Binding cancellationRejected(Queue loanEventsQueue,TopicExchange domainExchange){return BindingBuilder.bind(loanEventsQueue).to(domainExchange).with("prestamos.cancelacion-rechazada.v1");}
  @Bean Binding earlyRelease(Queue loanEventsQueue,TopicExchange domainExchange){return BindingBuilder.bind(loanEventsQueue).to(domainExchange).with("prestamos.periodo-liberado.v1");}
  @Bean Binding dlqBinding(Queue loanEventsDlq,DirectExchange deadLetterExchange){return BindingBuilder.bind(loanEventsDlq).to(deadLetterExchange).with(DLQ);}
}
