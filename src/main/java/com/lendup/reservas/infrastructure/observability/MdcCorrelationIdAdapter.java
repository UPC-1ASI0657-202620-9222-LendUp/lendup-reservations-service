package com.lendup.reservas.infrastructure.observability;

import com.lendup.reservas.application.port.out.CorrelationIdPort;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

@Component
public class MdcCorrelationIdAdapter implements CorrelationIdPort {
  @Override
  public String currentId() {
    String value = MDC.get("correlation_id");
    return value == null ? "" : value;
  }
}
