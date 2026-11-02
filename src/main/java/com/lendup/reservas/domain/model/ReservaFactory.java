package com.lendup.reservas.domain.model;

import java.util.Objects;
import java.util.UUID;

public final class ReservaFactory {
  public Reserva crearConfirmada(UUID reservaId, Solicitud solicitud, AgendaDelObjeto agenda) {
    Objects.requireNonNull(solicitud, "La solicitud es obligatoria");
    Objects.requireNonNull(agenda, "La agenda es obligatoria");
    if (solicitud.estado() != EstadoSolicitud.PENDIENTE) {
      throw new IllegalStateException("Solo una solicitud pendiente puede originar una reserva");
    }
    if (!solicitud.publicacionId().equals(agenda.publicacionId())) {
      throw new IllegalArgumentException("La agenda no corresponde a la publicación solicitada");
    }
    agenda.exigirDisponible(solicitud.periodo());
    return new Reserva(
        Objects.requireNonNull(reservaId, "El ID de reserva es obligatorio"),
        solicitud.id(),
        agenda.id(),
        solicitud.periodo(),
        solicitud.condiciones(),
        EstadoReserva.CONFIRMADA);
  }
}
