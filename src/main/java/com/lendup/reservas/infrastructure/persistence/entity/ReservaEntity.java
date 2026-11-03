package com.lendup.reservas.infrastructure.persistence.entity;

import com.lendup.reservas.domain.model.*;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.UUID;

@Entity
@Table(name = "reservas")
public class ReservaEntity {
  @Id private UUID id;
  @OneToOne(fetch = FetchType.EAGER, optional = false)
  @JoinColumn(name = "solicitud_id", nullable = false, unique = true)
  private SolicitudEntity solicitud;
  @Column(name = "agenda_id", nullable = false) private UUID agendaId;
  @Column(nullable = false) private LocalDateTime desde;
  @Column(nullable = false) private LocalDateTime hasta;
  @Column(name = "ocupacion_desde", nullable = false) private LocalDateTime ocupacionDesde;
  @Column(name = "ocupacion_hasta", nullable = false) private LocalDateTime ocupacionHasta;
  @Column(name = "periodo_liberado_desde") private LocalDateTime periodoLiberadoDesde;
  @Enumerated(EnumType.STRING) @Column(nullable = false) private EstadoReserva estado;
  @Column(name = "tarifa_diaria_acordada", nullable = false, precision = 12, scale = 2) private BigDecimal tarifaDiariaAcordada;
  @Column(name = "garantia_monetaria_acordada", precision = 12, scale = 2) private BigDecimal garantiaMonetariaAcordada;
  @Column(name = "moneda_acordada", nullable = false, length = 3) private String monedaAcordada;
  @Column(name = "lugar_intercambio_acordado", nullable = false) private String lugarIntercambioAcordado;
  @Column(name = "condiciones_uso_acordadas", nullable = false, columnDefinition = "TEXT") private String condicionesUsoAcordadas;
  @Column(name = "condiciones_entrega_acordadas", nullable = false, columnDefinition = "TEXT") private String condicionesEntregaAcordadas;
  @Column(name = "condiciones_devolucion_acordadas", nullable = false, columnDefinition = "TEXT") private String condicionesDevolucionAcordadas;
  @Column(name = "condiciones_cancelacion_acordadas", nullable = false, columnDefinition = "TEXT") private String condicionesCancelacionAcordadas;
  @Column(name = "condiciones_snapshot_id", length = 100) private String condicionesSnapshotId;
  @Column(name = "condiciones_snapshot_hash", length = 64) private String condicionesSnapshotHash;
  @Column(name = "condiciones_aceptadas_en") private Instant condicionesAceptadasEn;
  @Column(name = "confirmada_en", nullable = false) private Instant confirmadaEn;
  @Column(name = "cancelacion_solicitada_en") private Instant cancelacionSolicitadaEn;
  @Column(name = "cancelada_en") private Instant canceladaEn;
  @Column(name = "cancelada_por_usuario_id") private UUID canceladaPorUsuarioId;
  @Column(name = "motivo_cancelacion", columnDefinition = "TEXT") private String motivoCancelacion;
  @Column(name = "external_event_version", nullable = false) private long externalEventVersion;
  @Column(name = "aggregate_version", nullable = false) private long aggregateVersion;
  @Column(name = "actualizada_en", nullable = false) private Instant actualizadaEn;

  protected ReservaEntity() {}

  public static ReservaEntity fromDomain(Reserva reserva, SolicitudEntity solicitud) {
    ReservaEntity entity = new ReservaEntity();
    entity.id = reserva.id();
    entity.solicitud = solicitud;
    entity.apply(reserva);
    return entity;
  }

  public void apply(Reserva reserva) {
    agendaId = reserva.agendaId();
    desde = reserva.periodoContractual().desde();
    hasta = reserva.periodoContractual().hasta();
    ocupacionDesde = reserva.periodoContractual().desde();
    ocupacionHasta = reserva.ocupacionHasta();
    periodoLiberadoDesde = reserva.periodoLiberadoDesde();
    estado = reserva.estado();
    CondicionesAcordadas c = reserva.condiciones();
    tarifaDiariaAcordada = c.tarifaDiaria();
    garantiaMonetariaAcordada = c.garantiaMonetaria();
    monedaAcordada = c.moneda();
    lugarIntercambioAcordado = c.lugarIntercambio();
    condicionesUsoAcordadas = c.uso();
    condicionesEntregaAcordadas = c.entrega();
    condicionesDevolucionAcordadas = c.devolucion();
    condicionesCancelacionAcordadas = c.cancelacion();
    AceptacionCondiciones a = reserva.aceptacionCondiciones();
    condicionesSnapshotId = a == null ? null : a.snapshotId();
    condicionesSnapshotHash = a == null ? null : a.snapshotHash();
    condicionesAceptadasEn = a == null ? null : a.aceptadaEn();
    confirmadaEn = reserva.confirmadaEn();
    cancelacionSolicitadaEn = reserva.cancelacionSolicitadaEn();
    canceladaEn = reserva.canceladaEn();
    canceladaPorUsuarioId = reserva.canceladaPorUsuarioId();
    motivoCancelacion = reserva.motivoCancelacion();
    externalEventVersion = reserva.externalEventVersion();
    aggregateVersion = reserva.aggregateVersion();
    actualizadaEn = reserva.actualizadaEn();
  }

  public Reserva toDomain() {
    AceptacionCondiciones aceptacion = condicionesSnapshotId == null ? null
        : new AceptacionCondiciones(condicionesSnapshotId, condicionesSnapshotHash,
            condicionesAceptadasEn);
    return Reserva.rehidratar(id, solicitud.getId(), agendaId,
        new PeriodoPrestamo(desde, hasta), ocupacionHasta, periodoLiberadoDesde,
        new CondicionesAcordadas(tarifaDiariaAcordada, garantiaMonetariaAcordada,
            monedaAcordada, lugarIntercambioAcordado, condicionesUsoAcordadas,
            condicionesEntregaAcordadas, condicionesDevolucionAcordadas,
            condicionesCancelacionAcordadas), aceptacion, estado, confirmadaEn,
        cancelacionSolicitadaEn, canceladaEn, canceladaPorUsuarioId, motivoCancelacion,
        externalEventVersion, aggregateVersion, actualizadaEn);
  }

  public UUID getId() { return id; }
  public SolicitudEntity getSolicitud() { return solicitud; }
  public LocalDateTime getOcupacionDesde() { return ocupacionDesde; }
  public LocalDateTime getOcupacionHasta() { return ocupacionHasta; }
  public EstadoReserva getEstado() { return estado; }
}
