package com.lendup.reservas.infrastructure.persistence.entity;

import com.lendup.reservas.domain.model.EstadoReserva;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.UUID;

@Entity @Table(name="reservas")
public class ReservaEntity {
  @Id private UUID id;
  @OneToOne(fetch=FetchType.EAGER,optional=false) @JoinColumn(name="solicitud_id",nullable=false,unique=true) private SolicitudEntity solicitud;
  @Column(name="agenda_id",nullable=false) private UUID agendaId;
  @Column(nullable=false) private LocalDateTime desde;
  @Column(nullable=false) private LocalDateTime hasta;
  @Enumerated(EnumType.STRING) @Column(nullable=false) private EstadoReserva estado;
  @Column(name="tarifa_diaria_acordada",nullable=false,precision=12,scale=2) private BigDecimal tarifaDiariaAcordada;
  @Column(name="garantia_monetaria_acordada",precision=12,scale=2) private BigDecimal garantiaMonetariaAcordada;
  @Column(name="moneda_acordada",nullable=false,length=3) private String monedaAcordada;
  @Column(name="lugar_intercambio_acordado",nullable=false) private String lugarIntercambioAcordado;
  @Column(name="condiciones_uso_acordadas",nullable=false,columnDefinition="TEXT") private String condicionesUsoAcordadas;
  @Column(name="condiciones_entrega_acordadas",nullable=false,columnDefinition="TEXT") private String condicionesEntregaAcordadas;
  @Column(name="condiciones_devolucion_acordadas",nullable=false,columnDefinition="TEXT") private String condicionesDevolucionAcordadas;
  @Column(name="condiciones_cancelacion_acordadas",nullable=false,columnDefinition="TEXT") private String condicionesCancelacionAcordadas;
  @Column(name="confirmada_en",nullable=false) private Instant confirmadaEn;
  @Column(name="cancelacion_solicitada_en") private Instant cancelacionSolicitadaEn;
  @Column(name="cancelada_en") private Instant canceladaEn;
  @Column(name="cancelada_por_usuario_id") private UUID canceladaPorUsuarioId;
  @Column(name="motivo_cancelacion",columnDefinition="TEXT") private String motivoCancelacion;
  @Column(name="external_event_version",nullable=false) private long externalEventVersion;
  @Column(name="actualizada_en",nullable=false) private Instant actualizadaEn;
  protected ReservaEntity(){}
  public static ReservaEntity confirmada(UUID id,SolicitudEntity s,UUID agendaId,Instant now){ReservaEntity r=new ReservaEntity();r.id=id;r.solicitud=s;r.agendaId=agendaId;r.desde=s.getDesde();r.hasta=s.getHasta();r.estado=EstadoReserva.CONFIRMADA;r.tarifaDiariaAcordada=s.getTarifaDiariaAceptada();r.garantiaMonetariaAcordada=s.getGarantiaMonetariaAceptada();r.monedaAcordada=s.getMonedaAceptada();r.lugarIntercambioAcordado=s.getLugarIntercambioAceptado();r.condicionesUsoAcordadas=s.getCondicionesUsoAceptadas();r.condicionesEntregaAcordadas=s.getCondicionesEntregaAceptadas();r.condicionesDevolucionAcordadas=s.getCondicionesDevolucionAceptadas();r.condicionesCancelacionAcordadas=s.getCondicionesCancelacionAceptadas();r.confirmadaEn=now;r.actualizadaEn=now;return r;}
  public void solicitarCancelacion(UUID actor,String motivo,Instant now){estado=EstadoReserva.CANCELACION_PENDIENTE;cancelacionSolicitadaEn=now;canceladaPorUsuarioId=actor;motivoCancelacion=motivo;actualizadaEn=now;}
  public boolean aplicarPrestamoCancelado(long version,Instant now){if(version<=externalEventVersion)return false;if(estado!=EstadoReserva.CANCELACION_PENDIENTE)return false;externalEventVersion=version;estado=EstadoReserva.CANCELADA;canceladaEn=now;actualizadaEn=now;return true;}
  public boolean aplicarCancelacionRechazada(long version,Instant now){if(version<=externalEventVersion)return false;if(estado!=EstadoReserva.CANCELACION_PENDIENTE)return false;externalEventVersion=version;estado=EstadoReserva.CONFIRMADA;cancelacionSolicitadaEn=null;actualizadaEn=now;return true;}
  public boolean aplicarLiberacion(long version,LocalDateTime nuevoHasta,Instant now){if(version<=externalEventVersion)return false;if(!nuevoHasta.isAfter(desde)||nuevoHasta.isAfter(hasta))return false;externalEventVersion=version;hasta=nuevoHasta;actualizadaEn=now;return true;}
  public UUID getId(){return id;} public SolicitudEntity getSolicitud(){return solicitud;} public UUID getAgendaId(){return agendaId;} public LocalDateTime getDesde(){return desde;} public LocalDateTime getHasta(){return hasta;} public EstadoReserva getEstado(){return estado;} public BigDecimal getTarifaDiariaAcordada(){return tarifaDiariaAcordada;} public BigDecimal getGarantiaMonetariaAcordada(){return garantiaMonetariaAcordada;} public String getMonedaAcordada(){return monedaAcordada;} public String getLugarIntercambioAcordado(){return lugarIntercambioAcordado;} public String getCondicionesUsoAcordadas(){return condicionesUsoAcordadas;} public String getCondicionesEntregaAcordadas(){return condicionesEntregaAcordadas;} public String getCondicionesDevolucionAcordadas(){return condicionesDevolucionAcordadas;} public String getCondicionesCancelacionAcordadas(){return condicionesCancelacionAcordadas;} public Instant getConfirmadaEn(){return confirmadaEn;} public Instant getCancelacionSolicitadaEn(){return cancelacionSolicitadaEn;} public Instant getCanceladaEn(){return canceladaEn;} public UUID getCanceladaPorUsuarioId(){return canceladaPorUsuarioId;} public String getMotivoCancelacion(){return motivoCancelacion;} public Instant getActualizadaEn(){return actualizadaEn;} public long getExternalEventVersion(){return externalEventVersion;}
}
