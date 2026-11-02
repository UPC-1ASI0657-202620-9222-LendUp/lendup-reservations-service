package com.lendup.reservas.application.port.out;

import com.lendup.reservas.infrastructure.persistence.entity.*;
import com.lendup.reservas.domain.model.AgendaDelObjeto;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ReservationPersistencePort {
  SolicitudEntity saveSolicitud(SolicitudEntity entity);
  Optional<SolicitudEntity> findSolicitud(UUID id);
  Optional<SolicitudEntity> lockSolicitud(UUID id);
  List<SolicitudEntity> findSolicitudes(UUID actor, boolean admin, String rol, String estado);
  void ensureAgenda(UUID publicacionId);
  AgendaEntity lockAgenda(UUID publicacionId);
  List<AgendaDelObjeto.Bloqueo> findBlockingPeriods(UUID agendaId);
  boolean existsCommittedOverlap(UUID publicacionId, LocalDateTime desde, LocalDateTime hasta);
  boolean existsPendingDuplicate(UUID publicacionId, UUID prestatarioId, LocalDateTime desde, LocalDateTime hasta);
  ReservaEntity saveReserva(ReservaEntity entity);
  Optional<ReservaEntity> findReserva(UUID id);
  Optional<ReservaEntity> lockReserva(UUID id);
  List<ReservaEntity> findReservas(UUID actor, boolean admin, String rol);
}
