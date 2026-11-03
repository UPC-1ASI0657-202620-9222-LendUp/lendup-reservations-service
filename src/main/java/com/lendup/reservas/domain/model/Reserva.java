package com.lendup.reservas.domain.model;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

public final class Reserva {
  private final UUID id;
  private final UUID solicitudId;
  private final UUID agendaId;
  private final PeriodoPrestamo periodoContractual;
  private final CondicionesAcordadas condiciones;
  private final AceptacionCondiciones aceptacionCondiciones;
  private final Instant confirmadaEn;
  private EstadoReserva estado;
  private LocalDateTime ocupacionHasta;
  private LocalDateTime periodoLiberadoDesde;
  private Instant cancelacionSolicitadaEn;
  private Instant canceladaEn;
  private UUID canceladaPorUsuarioId;
  private String motivoCancelacion;
  private long externalEventVersion;
  private long aggregateVersion;
  private Instant actualizadaEn;

  private Reserva(UUID id, UUID solicitudId, UUID agendaId, PeriodoPrestamo periodoContractual,
      LocalDateTime ocupacionHasta, LocalDateTime periodoLiberadoDesde,
      CondicionesAcordadas condiciones, AceptacionCondiciones aceptacionCondiciones,
      EstadoReserva estado, Instant confirmadaEn, Instant cancelacionSolicitadaEn,
      Instant canceladaEn, UUID canceladaPorUsuarioId, String motivoCancelacion,
      long externalEventVersion, long aggregateVersion, Instant actualizadaEn) {
    this.id = Objects.requireNonNull(id, "El ID de reserva es obligatorio");
    this.solicitudId = Objects.requireNonNull(solicitudId, "La solicitud es obligatoria");
    this.agendaId = Objects.requireNonNull(agendaId, "La agenda es obligatoria");
    this.periodoContractual = Objects.requireNonNull(periodoContractual, "El periodo contractual es obligatorio");
    this.ocupacionHasta = Objects.requireNonNull(ocupacionHasta, "El fin de ocupación es obligatorio");
    if (!ocupacionHasta.isAfter(periodoContractual.desde()) || ocupacionHasta.isAfter(periodoContractual.hasta())) {
      throw new IllegalArgumentException("El periodo de ocupación debe estar dentro del periodo contractual");
    }
    this.periodoLiberadoDesde = periodoLiberadoDesde;
    this.condiciones = Objects.requireNonNull(condiciones, "Las condiciones son obligatorias");
    this.aceptacionCondiciones = aceptacionCondiciones;
    this.estado = Objects.requireNonNull(estado, "El estado es obligatorio");
    this.confirmadaEn = Objects.requireNonNull(confirmadaEn, "La confirmación es obligatoria");
    this.cancelacionSolicitadaEn = cancelacionSolicitadaEn;
    this.canceladaEn = canceladaEn;
    this.canceladaPorUsuarioId = canceladaPorUsuarioId;
    this.motivoCancelacion = motivoCancelacion;
    if (externalEventVersion < 0 || aggregateVersion < 1) throw new IllegalArgumentException("Versiones inválidas");
    this.externalEventVersion = externalEventVersion;
    this.aggregateVersion = aggregateVersion;
    this.actualizadaEn = Objects.requireNonNull(actualizadaEn, "La actualización es obligatoria");
  }

  static Reserva confirmar(UUID id, UUID solicitudId, UUID agendaId,
      PeriodoPrestamo periodo, CondicionesAcordadas condiciones,
      AceptacionCondiciones aceptacion, Instant now) {
    Objects.requireNonNull(aceptacion, "La reserva requiere evidencia de aceptación");
    return new Reserva(id, solicitudId, agendaId, periodo, periodo.hasta(), null, condiciones,
        aceptacion, EstadoReserva.CONFIRMADA, now, null, null, null, null, 0, 1, now);
  }

  public static Reserva rehidratar(UUID id, UUID solicitudId, UUID agendaId,
      PeriodoPrestamo periodoContractual, LocalDateTime ocupacionHasta,
      LocalDateTime periodoLiberadoDesde, CondicionesAcordadas condiciones,
      AceptacionCondiciones aceptacion, EstadoReserva estado, Instant confirmadaEn,
      Instant cancelacionSolicitadaEn, Instant canceladaEn, UUID canceladaPorUsuarioId,
      String motivoCancelacion, long externalEventVersion, long aggregateVersion,
      Instant actualizadaEn) {
    return new Reserva(id, solicitudId, agendaId, periodoContractual, ocupacionHasta,
        periodoLiberadoDesde, condiciones, aceptacion, estado, confirmadaEn,
        cancelacionSolicitadaEn, canceladaEn, canceladaPorUsuarioId, motivoCancelacion,
        externalEventVersion, aggregateVersion, actualizadaEn);
  }

  public void solicitarCancelacion(UUID actor, String motivo, Instant now) {
    if (estado != EstadoReserva.CONFIRMADA) throw new IllegalStateException("Reserva no confirmada");
    if (motivo == null || motivo.isBlank()) throw new IllegalArgumentException("El motivo es obligatorio");
    estado = EstadoReserva.CANCELACION_PENDIENTE;
    cancelacionSolicitadaEn = now;
    canceladaPorUsuarioId = Objects.requireNonNull(actor);
    motivoCancelacion = motivo.trim();
    actualizar(now);
  }

  public boolean aplicarPrestamoCancelado(long version, Instant now) {
    if (!aceptarVersionExterna(version) || estado != EstadoReserva.CANCELACION_PENDIENTE) return false;
    externalEventVersion = version;
    estado = EstadoReserva.CANCELADA;
    canceladaEn = now;
    actualizar(now);
    return true;
  }

  public boolean aplicarCancelacionRechazada(long version, Instant now) {
    if (!aceptarVersionExterna(version) || estado != EstadoReserva.CANCELACION_PENDIENTE) return false;
    externalEventVersion = version;
    estado = EstadoReserva.CONFIRMADA;
    cancelacionSolicitadaEn = null;
    actualizar(now);
    return true;
  }

  public boolean aplicarLiberacionConfirmada(long version, LocalDateTime nuevoHasta, Instant now) {
    if (!aceptarVersionExterna(version) || estado != EstadoReserva.CONFIRMADA) return false;
    if (nuevoHasta == null || !nuevoHasta.isAfter(periodoContractual.desde())
        || !nuevoHasta.isBefore(ocupacionHasta)) return false;
    externalEventVersion = version;
    ocupacionHasta = nuevoHasta;
    periodoLiberadoDesde = nuevoHasta;
    actualizar(now);
    return true;
  }

  private boolean aceptarVersionExterna(long version) { return version > externalEventVersion; }

  private void actualizar(Instant now) {
    aggregateVersion = Math.incrementExact(aggregateVersion);
    actualizadaEn = Objects.requireNonNull(now);
  }

  public UUID id() { return id; }
  public UUID solicitudId() { return solicitudId; }
  public UUID agendaId() { return agendaId; }
  public PeriodoPrestamo periodoContractual() { return periodoContractual; }
  public PeriodoPrestamo periodoOcupacion() { return new PeriodoPrestamo(periodoContractual.desde(), ocupacionHasta); }
  public LocalDateTime ocupacionHasta() { return ocupacionHasta; }
  public LocalDateTime periodoLiberadoDesde() { return periodoLiberadoDesde; }
  public CondicionesAcordadas condiciones() { return condiciones; }
  public AceptacionCondiciones aceptacionCondiciones() { return aceptacionCondiciones; }
  public EstadoReserva estado() { return estado; }
  public Instant confirmadaEn() { return confirmadaEn; }
  public Instant cancelacionSolicitadaEn() { return cancelacionSolicitadaEn; }
  public Instant canceladaEn() { return canceladaEn; }
  public UUID canceladaPorUsuarioId() { return canceladaPorUsuarioId; }
  public String motivoCancelacion() { return motivoCancelacion; }
  public long externalEventVersion() { return externalEventVersion; }
  public long aggregateVersion() { return aggregateVersion; }
  public Instant actualizadaEn() { return actualizadaEn; }
}
