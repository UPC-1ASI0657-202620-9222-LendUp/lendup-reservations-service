package com.lendup.reservas.application.service;

import com.lendup.reservas.application.exception.*;
import com.lendup.reservas.application.port.in.ReservationUseCases;
import com.lendup.reservas.application.port.out.*;
import com.lendup.reservas.domain.event.EventEnvelope;
import com.lendup.reservas.domain.model.*;
import com.lendup.reservas.domain.specification.SolicitudSpecifications;
import com.lendup.reservas.domain.specification.SolicitudSpecifications.*;
import com.lendup.reservas.infrastructure.persistence.entity.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReservationApplicationService implements ReservationUseCases {
  private final ReservationPersistencePort persistence;
  private final CatalogoPort catalogo;
  private final IdentidadPort identidad;
  private final OutboxPort outbox;
  private final AuditPort audit;
  private final Clock clock;
  private final ReservaFactory reservaFactory;

  public ReservationApplicationService(ReservationPersistencePort persistence, CatalogoPort catalogo, IdentidadPort identidad,
      OutboxPort outbox, AuditPort audit, Clock clock, ReservaFactory reservaFactory) {
    this.persistence=persistence; this.catalogo=catalogo; this.identidad=identidad; this.outbox=outbox; this.audit=audit; this.clock=clock; this.reservaFactory=reservaFactory;
  }

  @Override @Transactional
  public SolicitudView crearSolicitud(UUID actor, UUID publicacionId, LocalDateTime desde, LocalDateTime hasta) {
    PeriodoPrestamo periodo = new PeriodoPrestamo(desde, hasta);
    CatalogoPort.PublicacionSnapshot pub = catalogo.obtenerSnapshot(publicacionId, periodo);
    var eligibility = new ElegibilidadSolicitud(
        EstadoComprobacion.CUMPLE,
        EstadoComprobacion.DESCONOCIDO,
        EstadoComprobacion.DESCONOCIDO,
        EstadoComprobacion.DESCONOCIDO,
        pub.activa() ? EstadoComprobacion.CUMPLE : EstadoComprobacion.NO_CUMPLE,
        pub.periodoOfrecido() ? EstadoComprobacion.CUMPLE : EstadoComprobacion.NO_CUMPLE);
    var catalogRules = SolicitudSpecifications.publicacionActiva()
        .and(SolicitudSpecifications.periodoDisponible())
        .evaluate(eligibility);
    if (catalogRules.violations().contains("PUBLICACION_NO_ACTIVA")) throw new BusinessConflictException("Objeto no disponible");
    if (catalogRules.violations().contains("PERIODO_NO_DISPONIBLE")) throw new BusinessConflictException("Periodo fuera de la disponibilidad publicada");
    if (actor.equals(pub.propietarioId())) throw new BusinessConflictException("No puedes solicitar tu propio objeto");
    if (persistence.existsCommittedOverlap(publicacionId, desde, hasta)) throw new BusinessConflictException("Periodo ya reservado");
    if (persistence.existsPendingDuplicate(publicacionId, actor, desde, hasta)) throw new BusinessConflictException("Ya existe una solicitud pendiente para el mismo periodo");
    Instant now=clock.instant();
    SolicitudEntity entity=SolicitudEntity.nueva(UUID.randomUUID(), pub.id(), actor, pub.propietarioId(), periodo, pub.condiciones(), now);
    persistence.saveSolicitud(entity);
    append("SolicitudCreada", entity.getId(), 1, Map.of("solicitud_id",entity.getId(),"publicacion_id",publicacionId,
        "prestatario_usuario_id",actor,"prestamista_usuario_id",pub.propietarioId(),"desde",desde.toString(),"hasta",hasta.toString()), "");
    audit.record("SOLICITUD",entity.getId(),"CREAR",null,"PENDIENTE",actor,"");
    return SolicitudView.from(entity);
  }

  @Override @Transactional(readOnly=true)
  public List<SolicitudView> listarSolicitudes(UUID actor, boolean admin, String rol, String estado){
    return persistence.findSolicitudes(actor,admin,rol,estado).stream().map(SolicitudView::from).toList();
  }
  @Override @Transactional(readOnly=true)
  public SolicitudView obtenerSolicitud(UUID actor, boolean admin, UUID id){
    SolicitudEntity s=solicitud(id); authorize(actor,admin,s); return SolicitudView.from(s);
  }

  @Override @Transactional
  public ReservaView aceptarSolicitud(UUID actor, UUID id) {
    SolicitudEntity s=persistence.lockSolicitud(id).orElseThrow(()->new NotFoundException("Solicitud no encontrada"));
    if(!actor.equals(s.getPrestamistaUsuarioId())) throw new ForbiddenException("Solo el prestamista puede aceptar");
    if(s.getEstado()!=EstadoSolicitud.PENDIENTE) throw new BusinessConflictException("Solicitud no pendiente");
    persistence.ensureAgenda(s.getPublicacionId());
    AgendaEntity agendaRow=persistence.lockAgenda(s.getPublicacionId());
    AgendaDelObjeto agenda=new AgendaDelObjeto(agendaRow.getId(),agendaRow.getPublicacionId(),agendaRow.getVersion(),persistence.findBlockingPeriods(agendaRow.getId()));
    Instant now=clock.instant();
    Reserva domain;
    try { domain=reservaFactory.crearConfirmada(UUID.randomUUID(),s.toDomain(),agenda); }
    catch(IllegalStateException ex){throw new BusinessConflictException(ex.getMessage());}
    ReservaEntity r=ReservaEntity.confirmada(domain,s,now);
    try { persistence.saveReserva(r); } catch(DataIntegrityViolationException ex){ throw new BusinessConflictException("La solicitud ya tiene reserva"); }
    s.aceptar(now);
    persistence.saveSolicitud(s);
    append("ReservaConfirmada",r.getId(),r.getAggregateVersion(),reservaConfirmadaPayload(r,s),"");
    audit.record("SOLICITUD",s.getId(),"ACEPTAR","PENDIENTE","ACEPTADA",actor,"");
    audit.record("RESERVA",r.getId(),"CONFIRMAR",null,"CONFIRMADA",actor,"");
    return ReservaView.from(r,s);
  }

  @Override @Transactional
  public SolicitudView rechazarSolicitud(UUID actor, UUID id, String motivo){
    SolicitudEntity s=persistence.lockSolicitud(id).orElseThrow(()->new NotFoundException("Solicitud no encontrada"));
    if(!actor.equals(s.getPrestamistaUsuarioId())) throw new ForbiddenException("Solo el prestamista puede rechazar");
    EstadoSolicitud prev=s.getEstado();
    try{s.rechazar(clock.instant(),motivo);}catch(IllegalStateException e){throw new BusinessConflictException(e.getMessage());}
    persistence.saveSolicitud(s); audit.record("SOLICITUD",id,"RECHAZAR",prev.name(),s.getEstado().name(),actor,""); return SolicitudView.from(s);
  }

  @Override @Transactional
  public SolicitudView cancelarSolicitud(UUID actor, UUID id, String motivo){
    SolicitudEntity s=persistence.lockSolicitud(id).orElseThrow(()->new NotFoundException("Solicitud no encontrada"));
    if(!actor.equals(s.getPrestatarioUsuarioId())) throw new ForbiddenException("Solo el prestatario puede cancelar");
    EstadoSolicitud prev=s.getEstado();
    try{s.cancelar(clock.instant(),motivo);}catch(IllegalStateException e){throw new BusinessConflictException(e.getMessage());}
    persistence.saveSolicitud(s); audit.record("SOLICITUD",id,"CANCELAR",prev.name(),s.getEstado().name(),actor,""); return SolicitudView.from(s);
  }

  @Override @Transactional(readOnly=true)
  public List<ReservaView> listarReservas(UUID actor, boolean admin, String rol){
    return persistence.findReservas(actor,admin,rol).stream().map(r->ReservaView.from(r,r.getSolicitud())).toList();
  }

  @Override @Transactional
  public ReservaView solicitarCancelacionReserva(UUID actor, boolean admin, UUID id, String motivo, String correlationId){
    ReservaEntity r=persistence.lockReserva(id).orElseThrow(()->new NotFoundException("Reserva no encontrada"));
    authorize(actor,admin,r.getSolicitud());
    if(r.getEstado()!=EstadoReserva.CONFIRMADA) throw new BusinessConflictException("Reserva no confirmada");
    r.solicitarCancelacion(actor,motivo,clock.instant()); persistence.saveReserva(r);
    append("CancelacionReservaSolicitada",id,r.getAggregateVersion(),Map.of("reserva_id",id,"solicitud_id",r.getSolicitud().getId(),
        "solicitada_por_usuario_id",actor,"motivo_cancelacion",motivo==null?"":motivo),correlationId);
    audit.record("RESERVA",id,"SOLICITAR_CANCELACION","CONFIRMADA","CANCELACION_PENDIENTE",actor,correlationId);
    return ReservaView.from(r,r.getSolicitud());
  }

  @Override @Transactional(readOnly=true)
  public ContactoView obtenerContacto(UUID actor, boolean admin, UUID id){
    ReservaEntity r=persistence.findReserva(id).orElseThrow(()->new NotFoundException("Reserva no encontrada"));
    authorize(actor,admin,r.getSolicitud());
    if(r.getEstado()!=EstadoReserva.CONFIRMADA && r.getEstado()!=EstadoReserva.CANCELACION_PENDIENTE) throw new ForbiddenException("Contacto no disponible");
    UUID other=actor.equals(r.getSolicitud().getPrestatarioUsuarioId())?r.getSolicitud().getPrestamistaUsuarioId():r.getSolicitud().getPrestatarioUsuarioId();
    return new ContactoView(identidad.obtenerTelefonoAutorizado(other,id));
  }

  private SolicitudEntity solicitud(UUID id){return persistence.findSolicitud(id).orElseThrow(()->new NotFoundException("Solicitud no encontrada"));}
  private void authorize(UUID actor,boolean admin,SolicitudEntity s){if(!admin&&!actor.equals(s.getPrestatarioUsuarioId())&&!actor.equals(s.getPrestamistaUsuarioId()))throw new ForbiddenException("No autorizado");}
  private void append(String type,UUID aggregateId,long aggregateVersion,Map<String,Object> payload,String correlationId){outbox.append(new EventEnvelope(UUID.randomUUID(),type,1,aggregateVersion,aggregateId,clock.instant(),correlationId==null?"":correlationId,payload));}
  private Map<String,Object> reservaConfirmadaPayload(ReservaEntity r,SolicitudEntity s){
    Map<String,Object> p=new LinkedHashMap<>(); p.put("reserva_id",r.getId());p.put("solicitud_id",s.getId());p.put("publicacion_id",s.getPublicacionId());
    p.put("prestamista_usuario_id",s.getPrestamistaUsuarioId());p.put("prestatario_usuario_id",s.getPrestatarioUsuarioId());p.put("desde",r.getDesde().toString());p.put("hasta",r.getHasta().toString());
    p.put("tarifa_diaria_acordada",r.getTarifaDiariaAcordada());p.put("garantia_monetaria_acordada",r.getGarantiaMonetariaAcordada());p.put("moneda_acordada",r.getMonedaAcordada());
    p.put("lugar_intercambio_acordado",r.getLugarIntercambioAcordado());p.put("condiciones_uso_acordadas",r.getCondicionesUsoAcordadas());p.put("condiciones_entrega_acordadas",r.getCondicionesEntregaAcordadas());p.put("condiciones_devolucion_acordadas",r.getCondicionesDevolucionAcordadas());p.put("condiciones_cancelacion_acordadas",r.getCondicionesCancelacionAcordadas());return p;
  }

  public record SolicitudView(UUID id,UUID publicacionId,UUID prestatarioUsuarioId,UUID prestamistaUsuarioId,LocalDateTime desde,LocalDateTime hasta,String estado,
      BigDecimal tarifaDiariaAceptada,BigDecimal garantiaMonetariaAceptada,String monedaAceptada,String lugarIntercambioAceptado,String condicionesUsoAceptadas,
      String condicionesEntregaAceptadas,String condicionesDevolucionAceptadas,String condicionesCancelacionAceptadas,Instant condicionesAceptadasEn,Instant creadaEn,Instant respondidaEn,
      String motivoRechazo,Instant canceladaEn,String motivoCancelacion,Instant actualizadaEn){
    public static SolicitudView from(SolicitudEntity s){return new SolicitudView(s.getId(),s.getPublicacionId(),s.getPrestatarioUsuarioId(),s.getPrestamistaUsuarioId(),s.getDesde(),s.getHasta(),s.getEstado().name(),s.getTarifaDiariaAceptada(),s.getGarantiaMonetariaAceptada(),s.getMonedaAceptada(),s.getLugarIntercambioAceptado(),s.getCondicionesUsoAceptadas(),s.getCondicionesEntregaAceptadas(),s.getCondicionesDevolucionAceptadas(),s.getCondicionesCancelacionAceptadas(),s.isConsentimientoVerificado()?s.getCondicionesAceptadasEn():null,s.getCreadaEn(),s.getRespondidaEn(),s.getMotivoRechazo(),s.getCanceladaEn(),s.getMotivoCancelacion(),s.getActualizadaEn());}
  }
  public record ReservaView(UUID id,UUID solicitudId,UUID agendaId,LocalDateTime desde,LocalDateTime hasta,String estado,BigDecimal tarifaDiariaAcordada,BigDecimal garantiaMonetariaAcordada,String monedaAcordada,String lugarIntercambioAcordado,String condicionesUsoAcordadas,String condicionesEntregaAcordadas,String condicionesDevolucionAcordadas,String condicionesCancelacionAcordadas,Instant confirmadaEn,Instant cancelacionSolicitadaEn,Instant canceladaEn,UUID canceladaPorUsuarioId,String motivoCancelacion,Instant actualizadaEn){
    public static ReservaView from(ReservaEntity r,SolicitudEntity ignored){return new ReservaView(r.getId(),r.getSolicitud().getId(),r.getAgendaId(),r.getDesde(),r.getHasta(),r.getEstado().name(),r.getTarifaDiariaAcordada(),r.getGarantiaMonetariaAcordada(),r.getMonedaAcordada(),r.getLugarIntercambioAcordado(),r.getCondicionesUsoAcordadas(),r.getCondicionesEntregaAcordadas(),r.getCondicionesDevolucionAcordadas(),r.getCondicionesCancelacionAcordadas(),r.getConfirmadaEn(),r.getCancelacionSolicitadaEn(),r.getCanceladaEn(),r.getCanceladaPorUsuarioId(),r.getMotivoCancelacion(),r.getActualizadaEn());}
  }
  public record ContactoView(String telefono){}
}
