package com.lendup.reservas.domain.model;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class AgendaDelObjetoTest {
  private final LocalDateTime base = LocalDateTime.of(2026, 11, 10, 9, 0);

  @Test void permitePeriodosConsecutivosSemiabiertos() {
    var agenda = agenda(EstadoReserva.CONFIRMADA);
    assertTrue(agenda.estaDisponible(new PeriodoPrestamo(base.plusDays(1), base.plusDays(2))));
  }

  @Test void confirmadaYCancellationPendienteMantienenElBloqueo() {
    assertFalse(agenda(EstadoReserva.CONFIRMADA).estaDisponible(new PeriodoPrestamo(base.plusHours(1), base.plusHours(2))));
    assertFalse(agenda(EstadoReserva.CANCELACION_PENDIENTE).estaDisponible(new PeriodoPrestamo(base.plusHours(1), base.plusHours(2))));
  }

  @Test void canceladaLiberaElPeriodo() {
    assertTrue(agenda(EstadoReserva.CANCELADA).estaDisponible(new PeriodoPrestamo(base.plusHours(1), base.plusHours(2))));
  }

  private AgendaDelObjeto agenda(EstadoReserva state) {
    return new AgendaDelObjeto(UUID.randomUUID(), UUID.randomUUID(), 0,
        List.of(new AgendaDelObjeto.Bloqueo(UUID.randomUUID(), new PeriodoPrestamo(base, base.plusDays(1)), state)));
  }
}
