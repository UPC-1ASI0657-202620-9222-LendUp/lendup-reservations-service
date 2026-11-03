package com.lendup.reservas.domain.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public final class Solicitud {
  private final UUID id;
  private final UUID publicacionId;
  private final UUID prestatarioId;
  private final UUID prestamistaId;
  private final PeriodoPrestamo periodo;
  private final CondicionesAcordadas condiciones;
  private final AceptacionCondiciones aceptacionCondiciones;
  private final Instant creadaEn;
  private EstadoSolicitud estado;
  private Instant respondidaEn;
  private String motivoRechazo;
  private Instant canceladaEn;
  private String motivoCancelacion;
  private Instant actualizadaEn;
  private long aggregateVersion;

  private Solicitud(UUID id, UUID publicacionId, UUID prestatarioId, UUID prestamistaId,
      PeriodoPrestamo periodo, CondicionesAcordadas condiciones,
      AceptacionCondiciones aceptacionCondiciones, EstadoSolicitud estado, Instant creadaEn,
      Instant respondidaEn, String motivoRechazo, Instant canceladaEn,
      String motivoCancelacion, Instant actualizadaEn, long aggregateVersion) {
    this.id = Objects.requireNonNull(id, "El ID de solicitud es obligatorio");
    this.publicacionId = Objects.requireNonNull(publicacionId, "La publicación es obligatoria");
    this.prestatarioId = Objects.requireNonNull(prestatarioId, "El prestatario es obligatorio");
    this.prestamistaId = Objects.requireNonNull(prestamistaId, "El prestamista es obligatorio");
    if (prestatarioId.equals(prestamistaId)) throw new IllegalArgumentException("No se puede solicitar un objeto propio");
    this.periodo = Objects.requireNonNull(periodo, "El periodo es obligatorio");
    this.condiciones = Objects.requireNonNull(condiciones, "Las condiciones son obligatorias");
    this.aceptacionCondiciones = aceptacionCondiciones;
    this.estado = Objects.requireNonNull(estado, "El estado es obligatorio");
    this.creadaEn = Objects.requireNonNull(creadaEn, "La fecha de creación es obligatoria");
    this.respondidaEn = respondidaEn;
    this.motivoRechazo = motivoRechazo;
    this.canceladaEn = canceladaEn;
    this.motivoCancelacion = motivoCancelacion;
    this.actualizadaEn = Objects.requireNonNull(actualizadaEn, "La fecha de actualización es obligatoria");
    if (aggregateVersion < 1) throw new IllegalArgumentException("aggregateVersion debe ser positivo");
    this.aggregateVersion = aggregateVersion;
  }

  public static Solicitud crear(UUID id, UUID publicacionId, UUID prestatarioId,
      UUID prestamistaId, PeriodoPrestamo periodo, CondicionesAcordadas condiciones,
      AceptacionCondiciones aceptacion, Instant now) {
    Objects.requireNonNull(aceptacion, "La aceptación verificable es obligatoria");
    return new Solicitud(id, publicacionId, prestatarioId, prestamistaId, periodo, condiciones,
        aceptacion, EstadoSolicitud.PENDIENTE, now, null, null, null, null, now, 1);
  }

  public static Solicitud rehidratar(UUID id, UUID publicacionId, UUID prestatarioId,
      UUID prestamistaId, PeriodoPrestamo periodo, CondicionesAcordadas condiciones,
      AceptacionCondiciones aceptacion, EstadoSolicitud estado, Instant creadaEn,
      Instant respondidaEn, String motivoRechazo, Instant canceladaEn,
      String motivoCancelacion, Instant actualizadaEn, long aggregateVersion) {
    return new Solicitud(id, publicacionId, prestatarioId, prestamistaId, periodo, condiciones,
        aceptacion, estado, creadaEn, respondidaEn, motivoRechazo, canceladaEn,
        motivoCancelacion, actualizadaEn, aggregateVersion);
  }

  public void aceptar(UUID actor, Instant now) {
    exigirPendiente();
    if (!prestamistaId.equals(actor)) throw new SecurityException("Solo el prestamista acepta");
    estado = EstadoSolicitud.ACEPTADA;
    respondidaEn = now;
    actualizar(now);
  }

  public void rechazar(UUID actor, Instant now, String motivo) {
    exigirPendiente();
    if (!prestamistaId.equals(actor)) throw new SecurityException("Solo el prestamista rechaza");
    estado = EstadoSolicitud.RECHAZADA;
    respondidaEn = now;
    motivoRechazo = requireReason(motivo);
    actualizar(now);
  }

  public void cancelar(UUID actor, Instant now, String motivo) {
    exigirPendiente();
    if (!prestatarioId.equals(actor)) throw new SecurityException("Solo el prestatario cancela");
    estado = EstadoSolicitud.CANCELADA;
    canceladaEn = now;
    motivoCancelacion = requireReason(motivo);
    actualizar(now);
  }

  private void actualizar(Instant now) {
    actualizadaEn = Objects.requireNonNull(now);
    aggregateVersion = Math.incrementExact(aggregateVersion);
  }

  private static String requireReason(String value) {
    if (value == null || value.isBlank()) throw new IllegalArgumentException("El motivo es obligatorio");
    return value.trim();
  }

  private void exigirPendiente() {
    if (estado != EstadoSolicitud.PENDIENTE) throw new IllegalStateException("Solicitud no pendiente");
  }

  public UUID id() { return id; }
  public UUID publicacionId() { return publicacionId; }
  public UUID prestatarioId() { return prestatarioId; }
  public UUID prestamistaId() { return prestamistaId; }
  public PeriodoPrestamo periodo() { return periodo; }
  public CondicionesAcordadas condiciones() { return condiciones; }
  public AceptacionCondiciones aceptacionCondiciones() { return aceptacionCondiciones; }
  public EstadoSolicitud estado() { return estado; }
  public Instant creadaEn() { return creadaEn; }
  public Instant respondidaEn() { return respondidaEn; }
  public String motivoRechazo() { return motivoRechazo; }
  public Instant canceladaEn() { return canceladaEn; }
  public String motivoCancelacion() { return motivoCancelacion; }
  public Instant actualizadaEn() { return actualizadaEn; }
  public long aggregateVersion() { return aggregateVersion; }
}
