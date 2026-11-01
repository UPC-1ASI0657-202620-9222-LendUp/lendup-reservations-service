package com.lendup.reservas.domain.model;

import java.time.Instant;
import java.util.UUID;

public final class Solicitud {
  private final UUID id;
  private final UUID publicacionId;
  private final UUID prestatarioId;
  private final UUID prestamistaId;
  private final PeriodoPrestamo periodo;
  private final CondicionesAcordadas condiciones;
  private EstadoSolicitud estado;

  public Solicitud(UUID id, UUID publicacionId, UUID prestatarioId, UUID prestamistaId,
                   PeriodoPrestamo periodo, CondicionesAcordadas condiciones, EstadoSolicitud estado) {
    if (prestatarioId.equals(prestamistaId)) throw new IllegalArgumentException("No se puede solicitar un objeto propio");
    this.id = id; this.publicacionId = publicacionId; this.prestatarioId = prestatarioId;
    this.prestamistaId = prestamistaId; this.periodo = periodo; this.condiciones = condiciones; this.estado = estado;
  }
  public void aceptar(UUID actor) { exigirPendiente(); if (!prestamistaId.equals(actor)) throw new SecurityException("Solo el prestamista acepta"); estado = EstadoSolicitud.ACEPTADA; }
  public void rechazar(UUID actor) { exigirPendiente(); if (!prestamistaId.equals(actor)) throw new SecurityException("Solo el prestamista rechaza"); estado = EstadoSolicitud.RECHAZADA; }
  public void cancelar(UUID actor) { exigirPendiente(); if (!prestatarioId.equals(actor)) throw new SecurityException("Solo el prestatario cancela"); estado = EstadoSolicitud.CANCELADA; }
  private void exigirPendiente() { if (estado != EstadoSolicitud.PENDIENTE) throw new IllegalStateException("Solicitud no pendiente"); }
  public UUID id(){ return id; } public UUID publicacionId(){ return publicacionId; } public UUID prestatarioId(){ return prestatarioId; }
  public UUID prestamistaId(){ return prestamistaId; } public PeriodoPrestamo periodo(){ return periodo; }
  public CondicionesAcordadas condiciones(){ return condiciones; } public EstadoSolicitud estado(){ return estado; }
}
