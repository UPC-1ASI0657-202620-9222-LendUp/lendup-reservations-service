package com.lendup.reservas.infrastructure.persistence.entity;

import com.lendup.reservas.domain.model.*;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.UUID;

@Entity @Table(name="solicitudes")
public class SolicitudEntity {
  @Id private UUID id;
  @Column(name="publicacion_id",nullable=false) private UUID publicacionId;
  @Column(name="prestatario_usuario_id",nullable=false) private UUID prestatarioUsuarioId;
  @Column(name="prestamista_usuario_id",nullable=false) private UUID prestamistaUsuarioId;
  @Column(nullable=false) private LocalDateTime desde;
  @Column(nullable=false) private LocalDateTime hasta;
  @Enumerated(EnumType.STRING) @Column(nullable=false) private EstadoSolicitud estado;
  @Column(name="tarifa_diaria_aceptada",nullable=false,precision=12,scale=2) private BigDecimal tarifaDiariaAceptada;
  @Column(name="garantia_monetaria_aceptada",precision=12,scale=2) private BigDecimal garantiaMonetariaAceptada;
  @Column(name="moneda_aceptada",nullable=false,length=3) private String monedaAceptada;
  @Column(name="lugar_intercambio_aceptado",nullable=false) private String lugarIntercambioAceptado;
  @Column(name="condiciones_uso_aceptadas",nullable=false,columnDefinition="TEXT") private String condicionesUsoAceptadas;
  @Column(name="condiciones_entrega_aceptadas",nullable=false,columnDefinition="TEXT") private String condicionesEntregaAceptadas;
  @Column(name="condiciones_devolucion_aceptadas",nullable=false,columnDefinition="TEXT") private String condicionesDevolucionAceptadas;
  @Column(name="condiciones_cancelacion_aceptadas",nullable=false,columnDefinition="TEXT") private String condicionesCancelacionAceptadas;
  @Column(name="condiciones_aceptadas_en") private Instant condicionesAceptadasEn;
  @Column(name="consentimiento_verificado",nullable=false) private boolean consentimientoVerificado;
  @Column(name="creada_en",nullable=false) private Instant creadaEn;
  @Column(name="respondida_en") private Instant respondidaEn;
  @Column(name="motivo_rechazo",columnDefinition="TEXT") private String motivoRechazo;
  @Column(name="cancelada_en") private Instant canceladaEn;
  @Column(name="motivo_cancelacion",columnDefinition="TEXT") private String motivoCancelacion;
  @Column(name="actualizada_en",nullable=false) private Instant actualizadaEn;
  protected SolicitudEntity(){}
  public static SolicitudEntity nueva(UUID id,UUID publicacionId,UUID prestatarioId,UUID prestamistaId,PeriodoPrestamo p,CondicionesAcordadas c,Instant now){
    SolicitudEntity s=new SolicitudEntity();s.id=id;s.publicacionId=publicacionId;s.prestatarioUsuarioId=prestatarioId;s.prestamistaUsuarioId=prestamistaId;s.desde=p.desde();s.hasta=p.hasta();s.estado=EstadoSolicitud.PENDIENTE;s.tarifaDiariaAceptada=c.tarifaDiaria();s.garantiaMonetariaAceptada=c.garantiaMonetaria();s.monedaAceptada=c.moneda();s.lugarIntercambioAceptado=c.lugarIntercambio();s.condicionesUsoAceptadas=c.uso();s.condicionesEntregaAceptadas=c.entrega();s.condicionesDevolucionAceptadas=c.devolucion();s.condicionesCancelacionAceptadas=c.cancelacion();s.condicionesAceptadasEn=null;s.consentimientoVerificado=false;s.creadaEn=now;s.actualizadaEn=now;return s;
  }
  public void aceptar(Instant now){exigirPendiente();estado=EstadoSolicitud.ACEPTADA;respondidaEn=now;actualizadaEn=now;}
  public void rechazar(Instant now,String motivo){exigirPendiente();estado=EstadoSolicitud.RECHAZADA;respondidaEn=now;motivoRechazo=motivo;actualizadaEn=now;}
  public void cancelar(Instant now,String motivo){exigirPendiente();estado=EstadoSolicitud.CANCELADA;canceladaEn=now;motivoCancelacion=motivo;actualizadaEn=now;}
  private void exigirPendiente(){if(estado!=EstadoSolicitud.PENDIENTE)throw new IllegalStateException("Solicitud no pendiente");}
  public UUID getId(){return id;} public UUID getPublicacionId(){return publicacionId;} public UUID getPrestatarioUsuarioId(){return prestatarioUsuarioId;} public UUID getPrestamistaUsuarioId(){return prestamistaUsuarioId;} public LocalDateTime getDesde(){return desde;} public LocalDateTime getHasta(){return hasta;} public EstadoSolicitud getEstado(){return estado;} public BigDecimal getTarifaDiariaAceptada(){return tarifaDiariaAceptada;} public BigDecimal getGarantiaMonetariaAceptada(){return garantiaMonetariaAceptada;} public String getMonedaAceptada(){return monedaAceptada;} public String getLugarIntercambioAceptado(){return lugarIntercambioAceptado;} public String getCondicionesUsoAceptadas(){return condicionesUsoAceptadas;} public String getCondicionesEntregaAceptadas(){return condicionesEntregaAceptadas;} public String getCondicionesDevolucionAceptadas(){return condicionesDevolucionAceptadas;} public String getCondicionesCancelacionAceptadas(){return condicionesCancelacionAceptadas;} public Instant getCondicionesAceptadasEn(){return condicionesAceptadasEn;} public boolean isConsentimientoVerificado(){return consentimientoVerificado;} public Instant getCreadaEn(){return creadaEn;} public Instant getRespondidaEn(){return respondidaEn;} public String getMotivoRechazo(){return motivoRechazo;} public Instant getCanceladaEn(){return canceladaEn;} public String getMotivoCancelacion(){return motivoCancelacion;} public Instant getActualizadaEn(){return actualizadaEn;}
  public Solicitud toDomain(){return new Solicitud(id,publicacionId,prestatarioUsuarioId,prestamistaUsuarioId,new PeriodoPrestamo(desde,hasta),new CondicionesAcordadas(tarifaDiariaAceptada,garantiaMonetariaAceptada,monedaAceptada,lugarIntercambioAceptado,condicionesUsoAceptadas,condicionesEntregaAceptadas,condicionesDevolucionAceptadas,condicionesCancelacionAceptadas),estado);}
}
