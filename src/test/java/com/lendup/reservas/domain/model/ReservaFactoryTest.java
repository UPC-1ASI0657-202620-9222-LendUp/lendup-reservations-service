package com.lendup.reservas.domain.model;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import java.time.*;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ReservaFactoryTest {
  private final ReservaFactory factory = new ReservaFactory();
  private final Instant now = Instant.parse("2026-11-01T12:00:00Z");

  @Test void creaConfirmadaConIdsPeriodoSnapshotYEvidenciaDeLaSolicitud() {
    UUID publication = UUID.randomUUID(), agendaId = UUID.randomUUID();
    Solicitud request = request(publication, EstadoSolicitud.PENDIENTE, true);
    Reserva reservation = factory.crearConfirmada(UUID.randomUUID(), request,
        new AgendaDelObjeto(agendaId, publication, 0, List.of()), now);
    assertAll(
        () -> assertEquals(request.id(), reservation.solicitudId()),
        () -> assertEquals(agendaId, reservation.agendaId()),
        () -> assertEquals(request.periodo(), reservation.periodoContractual()),
        () -> assertEquals(request.condiciones(), reservation.condiciones()),
        () -> assertEquals(request.aceptacionCondiciones(), reservation.aceptacionCondiciones()),
        () -> assertEquals(EstadoReserva.CONFIRMADA, reservation.estado()));
  }

  @Test void rechazaSolicitudNoPendienteAgendaAjenaSolapamientoOSinConsentimiento() {
    UUID publication = UUID.randomUUID();
    assertThrows(IllegalStateException.class, () -> factory.crearConfirmada(UUID.randomUUID(),
        request(publication, EstadoSolicitud.ACEPTADA, true),
        new AgendaDelObjeto(UUID.randomUUID(), publication, 0, List.of()), now));
    assertThrows(IllegalArgumentException.class, () -> factory.crearConfirmada(UUID.randomUUID(),
        request(publication, EstadoSolicitud.PENDIENTE, true),
        new AgendaDelObjeto(UUID.randomUUID(), UUID.randomUUID(), 0, List.of()), now));
    PeriodoPrestamo period = period();
    AgendaDelObjeto blocked = new AgendaDelObjeto(UUID.randomUUID(), publication, 0,
        List.of(new AgendaDelObjeto.Bloqueo(UUID.randomUUID(), period, EstadoReserva.CONFIRMADA)));
    assertThrows(IllegalStateException.class, () -> factory.crearConfirmada(UUID.randomUUID(),
        request(publication, EstadoSolicitud.PENDIENTE, true), blocked, now));
    assertThrows(IllegalStateException.class, () -> factory.crearConfirmada(UUID.randomUUID(),
        request(publication, EstadoSolicitud.PENDIENTE, false),
        new AgendaDelObjeto(UUID.randomUUID(), publication, 0, List.of()), now));
  }

  private Solicitud request(UUID publication, EstadoSolicitud state, boolean consent) {
    AceptacionCondiciones acceptance = consent
        ? new AceptacionCondiciones("v1", "a".repeat(64), now) : null;
    return Solicitud.rehidratar(UUID.randomUUID(), publication, UUID.randomUUID(),
        UUID.randomUUID(), period(), terms(), acceptance, state, now, null, null,
        null, null, now, 1);
  }

  private PeriodoPrestamo period() {
    return new PeriodoPrestamo(LocalDateTime.of(2026, 11, 10, 9, 0),
        LocalDateTime.of(2026, 11, 11, 9, 0));
  }

  private CondicionesAcordadas terms() {
    return new CondicionesAcordadas(new BigDecimal("10.00"), null, "PEN", "Campus",
        "uso", "entrega", "devolución", "cancelación");
  }
}
