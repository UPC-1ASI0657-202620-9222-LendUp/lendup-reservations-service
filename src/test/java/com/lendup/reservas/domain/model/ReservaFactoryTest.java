package com.lendup.reservas.domain.model;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ReservaFactoryTest {
  private final ReservaFactory factory = new ReservaFactory();

  @Test void creaConfirmadaConIdsPeriodoYSnapshotDeLaSolicitud() {
    UUID publication = UUID.randomUUID(), agendaId = UUID.randomUUID(), reservationId = UUID.randomUUID();
    var terms = terms();
    var request = request(publication, terms, EstadoSolicitud.PENDIENTE);
    var reservation = factory.crearConfirmada(reservationId, request, new AgendaDelObjeto(agendaId, publication, 0, List.of()));
    assertAll(
        () -> assertEquals(reservationId, reservation.id()),
        () -> assertEquals(request.id(), reservation.solicitudId()),
        () -> assertEquals(agendaId, reservation.agendaId()),
        () -> assertEquals(EstadoReserva.CONFIRMADA, reservation.estado()),
        () -> assertSame(terms, reservation.condiciones()));
  }

  @Test void rechazaSolicitudNoPendienteAgendaAjenaYSolapamiento() {
    UUID publication = UUID.randomUUID();
    assertThrows(IllegalStateException.class, () -> factory.crearConfirmada(UUID.randomUUID(), request(publication, terms(), EstadoSolicitud.ACEPTADA), new AgendaDelObjeto(UUID.randomUUID(), publication, 0, List.of())));
    assertThrows(IllegalArgumentException.class, () -> factory.crearConfirmada(UUID.randomUUID(), request(publication, terms(), EstadoSolicitud.PENDIENTE), new AgendaDelObjeto(UUID.randomUUID(), UUID.randomUUID(), 0, List.of())));
    var period = period();
    var agenda = new AgendaDelObjeto(UUID.randomUUID(), publication, 0, List.of(new AgendaDelObjeto.Bloqueo(UUID.randomUUID(), period, EstadoReserva.CONFIRMADA)));
    assertThrows(IllegalStateException.class, () -> factory.crearConfirmada(UUID.randomUUID(), request(publication, terms(), EstadoSolicitud.PENDIENTE), agenda));
  }

  private Solicitud request(UUID publication, CondicionesAcordadas terms, EstadoSolicitud state) {
    return new Solicitud(UUID.randomUUID(), publication, UUID.randomUUID(), UUID.randomUUID(), period(), terms, state);
  }
  private PeriodoPrestamo period() { return new PeriodoPrestamo(LocalDateTime.of(2026, 11, 10, 9, 0), LocalDateTime.of(2026, 11, 11, 9, 0)); }
  private CondicionesAcordadas terms() { return new CondicionesAcordadas(new BigDecimal("10.00"), null, "PEN", "Campus", "uso", "entrega", "devolución", "cancelación"); }
}
