package com.lendup.reservas.application.service;

import com.lendup.reservas.application.exception.NotFoundException;
import com.lendup.reservas.application.port.out.ReservationPersistencePort;
import com.lendup.reservas.domain.model.AceptacionCondiciones;
import com.lendup.reservas.domain.model.CondicionesAcordadas;
import com.lendup.reservas.domain.model.PeriodoPrestamo;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReservationAgreementQuery {
  private final ReservationPersistencePort reservations;

  public ReservationAgreementQuery(ReservationPersistencePort reservations) {
    this.reservations = reservations;
  }

  public record Agreement(UUID id, PeriodoPrestamo periodo, CondicionesAcordadas condiciones,
                          AceptacionCondiciones aceptacion) {}

  @Transactional(readOnly = true)
  public Agreement obtener(UUID id) {
    var reserva = reservations.findReserva(id).orElseThrow(() -> new NotFoundException("Reserva no encontrada"));
    return new Agreement(reserva.id(), reserva.periodoContractual(), reserva.condiciones(), reserva.aceptacionCondiciones());
  }
}
