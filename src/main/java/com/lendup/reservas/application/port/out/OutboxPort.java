package com.lendup.reservas.application.port.out;
import com.lendup.reservas.domain.event.EventEnvelope;
public interface OutboxPort { void append(EventEnvelope event); }
