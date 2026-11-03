package com.lendup.reservas.infrastructure.persistence;

import com.lendup.reservas.application.exception.BusinessConflictException;
import com.lendup.reservas.application.exception.NotFoundException;
import com.lendup.reservas.application.port.out.ReservationPersistencePort;
import com.lendup.reservas.domain.model.*;
import com.lendup.reservas.infrastructure.persistence.entity.*;
import com.lendup.reservas.infrastructure.persistence.repository.*;
import java.time.LocalDateTime;
import java.util.*;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
public class JpaReservationAdapter implements ReservationPersistencePort {
  private final SolicitudJpaRepository solicitudes;
  private final AgendaJpaRepository agendas;
  private final ReservaJpaRepository reservas;

  public JpaReservationAdapter(SolicitudJpaRepository solicitudes,
      AgendaJpaRepository agendas, ReservaJpaRepository reservas) {
    this.solicitudes = solicitudes;
    this.agendas = agendas;
    this.reservas = reservas;
  }

  @Override
  public Solicitud saveSolicitud(Solicitud solicitud) {
    boolean nueva = !solicitudes.existsById(solicitud.id());
    SolicitudEntity entity = solicitudes.findById(solicitud.id())
        .orElseGet(() -> SolicitudEntity.fromDomain(solicitud));
    entity.apply(solicitud);
    try {
      return (nueva ? solicitudes.saveAndFlush(entity) : solicitudes.save(entity)).toDomain();
    } catch (DataIntegrityViolationException exception) {
      throw new BusinessConflictException("Ya existe una solicitud pendiente para el mismo periodo");
    }
  }

  @Override public Optional<Solicitud> findSolicitud(UUID id) {
    return solicitudes.findById(id).map(SolicitudEntity::toDomain);
  }

  @Override public Optional<Solicitud> lockSolicitud(UUID id) {
    return solicitudes.lockById(id).map(SolicitudEntity::toDomain);
  }

  @Override public List<Solicitud> findSolicitudes(UUID actor, boolean admin,
      String rol, String estado) {
    return solicitudes.search(actor, admin, normalizeRole(rol), estado == null ? "" : estado)
        .stream().map(SolicitudEntity::toDomain).toList();
  }

  @Override
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void ensureAgenda(UUID publicacionId) {
    agendas.insertIfAbsent(UUID.randomUUID().toString(), publicacionId.toString());
  }

  @Override public AgendaDelObjeto lockAgenda(UUID publicacionId) {
    AgendaEntity agenda = agendas.lockByPublicationId(publicacionId)
        .orElseThrow(() -> new NotFoundException("Agenda no inicializada"));
    List<AgendaDelObjeto.Bloqueo> bloqueos = reservas.findBlockingByAgendaId(agenda.getId())
        .stream().map(entity -> new AgendaDelObjeto.Bloqueo(entity.getId(),
            new PeriodoPrestamo(entity.getOcupacionDesde(), entity.getOcupacionHasta()),
            entity.getEstado())).toList();
    return new AgendaDelObjeto(agenda.getId(), agenda.getPublicacionId(), agenda.getVersion(),
        bloqueos);
  }

  @Override public boolean existsCommittedOverlap(UUID publicacionId, LocalDateTime desde,
      LocalDateTime hasta) {
    return reservas.existsCommittedOverlap(publicacionId, desde, hasta);
  }

  @Override public boolean existsPendingDuplicate(UUID publicacionId, UUID prestatarioId,
      LocalDateTime desde, LocalDateTime hasta) {
    return solicitudes.existsPendingDuplicate(publicacionId, prestatarioId, desde, hasta);
  }

  @Override public Reserva saveReserva(Reserva reserva, Solicitud solicitud) {
    ReservaEntity entity = reservas.findById(reserva.id()).orElse(null);
    if (entity == null) {
      SolicitudEntity request = solicitudes.findById(solicitud.id())
          .orElseThrow(() -> new NotFoundException("Solicitud no encontrada"));
      entity = ReservaEntity.fromDomain(reserva, request);
    } else {
      entity.apply(reserva);
    }
    try {
      return reservas.saveAndFlush(entity).toDomain();
    } catch (DataIntegrityViolationException exception) {
      throw new BusinessConflictException("La solicitud ya tiene reserva");
    }
  }

  @Override public Optional<Reserva> findReserva(UUID id) {
    return reservas.findById(id).map(ReservaEntity::toDomain);
  }

  @Override public Optional<Reserva> lockReserva(UUID id) {
    return reservas.lockById(id).map(ReservaEntity::toDomain);
  }

  @Override public Optional<Solicitud> findSolicitudForReserva(UUID reservaId) {
    return reservas.findById(reservaId).map(ReservaEntity::getSolicitud)
        .map(SolicitudEntity::toDomain);
  }

  @Override public List<Reserva> findReservas(UUID actor, boolean admin, String rol) {
    return reservas.search(actor, admin, normalizeRole(rol)).stream()
        .map(ReservaEntity::toDomain).toList();
  }

  private String normalizeRole(String rol) { return rol == null ? "" : rol; }
}
