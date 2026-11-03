package com.lendup.reservas.application.port.out;

import com.lendup.reservas.domain.model.AgendaDelObjeto;
import com.lendup.reservas.domain.model.Reserva;
import com.lendup.reservas.domain.model.Solicitud;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ReservationPersistencePort {
  Solicitud saveSolicitud(Solicitud solicitud);
  Optional<Solicitud> findSolicitud(UUID id);
  Optional<Solicitud> lockSolicitud(UUID id);
  List<Solicitud> findSolicitudes(UUID actor, boolean admin, String rol, String estado);
  void ensureAgenda(UUID publicacionId);
  AgendaDelObjeto lockAgenda(UUID publicacionId);
  boolean existsCommittedOverlap(UUID publicacionId, LocalDateTime desde, LocalDateTime hasta);
  boolean existsPendingDuplicate(UUID publicacionId, UUID prestatarioId,
      LocalDateTime desde, LocalDateTime hasta);
  Reserva saveReserva(Reserva reserva, Solicitud solicitud);
  Optional<Reserva> findReserva(UUID id);
  Optional<Reserva> lockReserva(UUID id);
  Optional<Solicitud> findSolicitudForReserva(UUID reservaId);
  List<Reserva> findReservas(UUID actor, boolean admin, String rol);
}
