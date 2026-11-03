package com.lendup.reservas.domain.model;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import java.time.*;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class AggregatesTest {
  private final Instant now = Instant.parse("2026-11-01T12:00:00Z");

  @Test void garantiaEsOpcionalYSnapshotEsInmutable() {
    Solicitud request = request(UUID.randomUUID(), UUID.randomUUID());
    Reserva reservation = new ReservaFactory().crearConfirmada(UUID.randomUUID(), request,
        new AgendaDelObjeto(UUID.randomUUID(), request.publicacionId(), 0, List.of()), now);
    assertAll(
        () -> assertNull(reservation.condiciones().garantiaMonetaria()),
        () -> assertEquals("uso", reservation.condiciones().uso()),
        () -> assertEquals(request.aceptacionCondiciones(), reservation.aceptacionCondiciones()));
  }

  @Test void solicitudSoloPuedeDecidirseUnaVez() {
    UUID borrower = UUID.randomUUID(), lender = UUID.randomUUID();
    Solicitud request = request(borrower, lender);
    request.aceptar(lender, now.plusSeconds(1));
    assertThrows(IllegalStateException.class,
        () -> request.aceptar(lender, now.plusSeconds(2)));
  }

  @Test void permisosContextualesNoSonRolesPermanentes() {
    UUID borrower = UUID.randomUUID(), lender = UUID.randomUUID();
    Solicitud request = request(borrower, lender);
    assertThrows(SecurityException.class,
        () -> request.aceptar(borrower, now.plusSeconds(1)));
  }

  @Test void cancelacionDistribuidaNoLiberaHastaConfirmacion() {
    Solicitud request = request(UUID.randomUUID(), UUID.randomUUID());
    Reserva reservation = new ReservaFactory().crearConfirmada(UUID.randomUUID(), request,
        new AgendaDelObjeto(UUID.randomUUID(), request.publicacionId(), 0, List.of()), now);
    PeriodoPrestamo occupied = reservation.periodoOcupacion();
    reservation.solicitarCancelacion(request.prestatarioId(), "Cambio", now.plusSeconds(1));
    assertEquals(occupied, reservation.periodoOcupacion());
    assertTrue(reservation.aplicarPrestamoCancelado(1, now.plusSeconds(2)));
    assertEquals(EstadoReserva.CANCELADA, reservation.estado());
  }

  @Test void liberacionAnticipadaNoModificaPeriodoContractualNiCondiciones() {
    Solicitud request = request(UUID.randomUUID(), UUID.randomUUID());
    Reserva reservation = new ReservaFactory().crearConfirmada(UUID.randomUUID(), request,
        new AgendaDelObjeto(UUID.randomUUID(), request.publicacionId(), 0, List.of()), now);
    PeriodoPrestamo original = reservation.periodoContractual();
    LocalDateTime releasedAt = original.hasta().minusHours(3);
    assertTrue(reservation.aplicarLiberacionConfirmada(1, releasedAt, now.plusSeconds(1)));
    assertEquals(original, reservation.periodoContractual());
    assertEquals(releasedAt, reservation.ocupacionHasta());
    assertEquals(request.condiciones(), reservation.condiciones());
    assertFalse(reservation.aplicarLiberacionConfirmada(1, releasedAt.minusHours(1), now.plusSeconds(2)));
  }

  @Test void condicionesRechazanBlancosLongitudesEImportesIncompatibles() {
    assertThrows(IllegalArgumentException.class, () -> new CondicionesAcordadas(
        BigDecimal.ONE, null, "PEN", " ", "u", "e", "d", "c"));
    assertThrows(IllegalArgumentException.class, () -> new CondicionesAcordadas(
        new BigDecimal("1.001"), null, "PEN", "Campus", "u", "e", "d", "c"));
    assertThrows(IllegalArgumentException.class, () -> new CondicionesAcordadas(
        BigDecimal.ONE, null, "pen", "Campus", "u", "e", "d", "c"));
  }

  @Test void aceptacionRequiereHashSha256YFechaDeServidor() {
    assertThrows(IllegalArgumentException.class,
        () -> new AceptacionCondiciones("v1", "no-es-hash", now));
    assertEquals(HASH, new AceptacionCondiciones("v1", HASH.toUpperCase(), now).snapshotHash());
  }

  private Solicitud request(UUID borrower, UUID lender) {
    return Solicitud.crear(UUID.randomUUID(), UUID.randomUUID(), borrower, lender,
        new PeriodoPrestamo(LocalDateTime.of(2026, 11, 10, 9, 0),
            LocalDateTime.of(2026, 11, 11, 9, 0)), terms(),
        new AceptacionCondiciones("catalog-v1", HASH, now), now);
  }

  private CondicionesAcordadas terms() {
    return new CondicionesAcordadas(new BigDecimal("10.00"), null, "PEN", "Campus",
        "uso", "entrega", "devolucion", "cancelacion");
  }

  private static final String HASH = "a".repeat(64);
}
