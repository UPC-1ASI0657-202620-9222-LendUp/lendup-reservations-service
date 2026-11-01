package com.lendup.reservas.infrastructure.persistence;

import com.lendup.reservas.application.exception.NotFoundException;
import com.lendup.reservas.application.port.out.ReservationPersistencePort;
import com.lendup.reservas.infrastructure.persistence.entity.*;
import com.lendup.reservas.infrastructure.persistence.repository.*;
import java.time.LocalDateTime;
import java.util.*;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
public class JpaReservationAdapter implements ReservationPersistencePort {
  private final SolicitudJpaRepository solicitudes; private final AgendaJpaRepository agendas; private final ReservaJpaRepository reservas;
  public JpaReservationAdapter(SolicitudJpaRepository s,AgendaJpaRepository a,ReservaJpaRepository r){solicitudes=s;agendas=a;reservas=r;}
  public SolicitudEntity saveSolicitud(SolicitudEntity e){return solicitudes.save(e);} public Optional<SolicitudEntity> findSolicitud(UUID id){return solicitudes.findById(id);} public Optional<SolicitudEntity> lockSolicitud(UUID id){return solicitudes.lockById(id);}
  public List<SolicitudEntity> findSolicitudes(UUID actor,boolean admin,String rol,String estado){return solicitudes.search(actor,admin,normalizeRole(rol),estado==null?"":estado);}
  @Transactional(propagation=Propagation.REQUIRES_NEW)
  public void ensureAgenda(UUID publicationId){agendas.insertIfAbsent(UUID.randomUUID().toString(),publicationId.toString());}
  public AgendaEntity lockAgenda(UUID publicationId){return agendas.lockByPublicationId(publicationId).orElseThrow(()->new NotFoundException("Agenda no inicializada"));}
  public boolean existsOverlap(UUID agendaId,LocalDateTime desde,LocalDateTime hasta){return reservas.existsOverlap(agendaId,desde,hasta);}
  public ReservaEntity saveReserva(ReservaEntity e){return reservas.save(e);} public Optional<ReservaEntity> findReserva(UUID id){return reservas.findById(id);} public Optional<ReservaEntity> lockReserva(UUID id){return reservas.lockById(id);}
  public List<ReservaEntity> findReservas(UUID actor,boolean admin,String rol){return reservas.search(actor,admin,normalizeRole(rol));}
  private String normalizeRole(String rol){return rol==null?"":rol;}
}
