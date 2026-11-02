package com.lendup.reservas.domain.model;

import java.util.Objects;
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
    Objects.requireNonNull(prestatarioId, "El prestatario es obligatorio");
    Objects.requireNonNull(prestamistaId, "El prestamista es obligatorio");
    if (prestatarioId.equals(prestamistaId)) throw new IllegalArgumentException("No se puede solicitar un objeto propio");
    this.id = Objects.requireNonNull(id, "El ID de solicitud es obligatorio");
    this.publicacionId = Objects.requireNonNull(publicacionId, "La publicación es obligatoria");
    this.prestatarioId = prestatarioId;
    this.prestamistaId = prestamistaId;
    this.periodo = Objects.requireNonNull(periodo, "El periodo es obligatorio");
    this.condiciones = Objects.requireNonNull(condiciones, "Las condiciones son obligatorias");
    this.estado = Objects.requireNonNull(estado, "El estado es obligatorio");
  }
  public void aceptar(UUID actor) { exigirPendiente(); if (!prestamistaId.equals(actor)) throw new SecurityException("Solo el prestamista acepta"); estado = EstadoSolicitud.ACEPTADA; }
  public void rechazar(UUID actor) { exigirPendiente(); if (!prestamistaId.equals(actor)) throw new SecurityException("Solo el prestamista rechaza"); estado = EstadoSolicitud.RECHAZADA; }
  public void cancelar(UUID actor) { exigirPendiente(); if (!prestatarioId.equals(actor)) throw new SecurityException("Solo el prestatario cancela"); estado = EstadoSolicitud.CANCELADA; }
  private void exigirPendiente() { if (estado != EstadoSolicitud.PENDIENTE) throw new IllegalStateException("Solicitud no pendiente"); }
  public UUID id(){ return id; } public UUID publicacionId(){ return publicacionId; } public UUID prestatarioId(){ return prestatarioId; }
  public UUID prestamistaId(){ return prestamistaId; } public PeriodoPrestamo periodo(){ return periodo; }
  public CondicionesAcordadas condiciones(){ return condiciones; } public EstadoSolicitud estado(){ return estado; }
}
