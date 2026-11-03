package com.lendup.reservas.infrastructure.persistence.entity;

import com.lendup.reservas.domain.model.*;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.*;
import java.util.UUID;

@Entity
@Table(name = "solicitudes")
public class SolicitudEntity {
  @Id private UUID id;
  @Column(name = "publicacion_id", nullable = false) private UUID publicacionId;
  @Column(name = "prestatario_usuario_id", nullable = false) private UUID prestatarioUsuarioId;
  @Column(name = "prestamista_usuario_id", nullable = false) private UUID prestamistaUsuarioId;
  @Column(nullable = false) private LocalDateTime desde;
  @Column(nullable = false) private LocalDateTime hasta;
  @Enumerated(EnumType.STRING) @Column(nullable = false) private EstadoSolicitud estado;
  @Column(name = "tarifa_diaria_aceptada", nullable = false, precision = 12, scale = 2) private BigDecimal tarifaDiariaAceptada;
  @Column(name = "garantia_monetaria_aceptada", precision = 12, scale = 2) private BigDecimal garantiaMonetariaAceptada;
  @Column(name = "moneda_aceptada", nullable = false, length = 3) private String monedaAceptada;
  @Column(name = "lugar_intercambio_aceptado", nullable = false) private String lugarIntercambioAceptado;
  @Column(name = "condiciones_uso_aceptadas", nullable = false, columnDefinition = "TEXT") private String condicionesUsoAceptadas;
  @Column(name = "condiciones_entrega_aceptadas", nullable = false, columnDefinition = "TEXT") private String condicionesEntregaAceptadas;
  @Column(name = "condiciones_devolucion_aceptadas", nullable = false, columnDefinition = "TEXT") private String condicionesDevolucionAceptadas;
  @Column(name = "condiciones_cancelacion_aceptadas", nullable = false, columnDefinition = "TEXT") private String condicionesCancelacionAceptadas;
  @Column(name = "condiciones_snapshot_id", length = 100) private String condicionesSnapshotId;
  @Column(name = "condiciones_snapshot_hash", length = 64) private String condicionesSnapshotHash;
  @Column(name = "condiciones_aceptadas_en") private Instant condicionesAceptadasEn;
  @Column(name = "consentimiento_verificado", nullable = false) private boolean consentimientoVerificado;
  @Column(name = "deduplication_key", columnDefinition = "BINARY(32)") private byte[] deduplicationKey;
  @Column(name = "aggregate_version", nullable = false) private long aggregateVersion;
  @Column(name = "creada_en", nullable = false) private Instant creadaEn;
  @Column(name = "respondida_en") private Instant respondidaEn;
  @Column(name = "motivo_rechazo", columnDefinition = "TEXT") private String motivoRechazo;
  @Column(name = "cancelada_en") private Instant canceladaEn;
  @Column(name = "motivo_cancelacion", columnDefinition = "TEXT") private String motivoCancelacion;
  @Column(name = "actualizada_en", nullable = false) private Instant actualizadaEn;

  protected SolicitudEntity() {}

  public static SolicitudEntity fromDomain(Solicitud solicitud) {
    SolicitudEntity entity = new SolicitudEntity();
    entity.id = solicitud.id();
    entity.apply(solicitud);
    return entity;
  }

  public void apply(Solicitud solicitud) {
    id = solicitud.id();
    publicacionId = solicitud.publicacionId();
    prestatarioUsuarioId = solicitud.prestatarioId();
    prestamistaUsuarioId = solicitud.prestamistaId();
    desde = solicitud.periodo().desde();
    hasta = solicitud.periodo().hasta();
    estado = solicitud.estado();
    CondicionesAcordadas condiciones = solicitud.condiciones();
    tarifaDiariaAceptada = condiciones.tarifaDiaria();
    garantiaMonetariaAceptada = condiciones.garantiaMonetaria();
    monedaAceptada = condiciones.moneda();
    lugarIntercambioAceptado = condiciones.lugarIntercambio();
    condicionesUsoAceptadas = condiciones.uso();
    condicionesEntregaAceptadas = condiciones.entrega();
    condicionesDevolucionAceptadas = condiciones.devolucion();
    condicionesCancelacionAceptadas = condiciones.cancelacion();
    AceptacionCondiciones aceptacion = solicitud.aceptacionCondiciones();
    condicionesSnapshotId = aceptacion == null ? null : aceptacion.snapshotId();
    condicionesSnapshotHash = aceptacion == null ? null : aceptacion.snapshotHash();
    condicionesAceptadasEn = aceptacion == null ? condicionesAceptadasEn : aceptacion.aceptadaEn();
    consentimientoVerificado = aceptacion != null;
    deduplicationKey = estado == EstadoSolicitud.PENDIENTE ? duplicateKey(solicitud) : null;
    aggregateVersion = solicitud.aggregateVersion();
    creadaEn = solicitud.creadaEn();
    respondidaEn = solicitud.respondidaEn();
    motivoRechazo = solicitud.motivoRechazo();
    canceladaEn = solicitud.canceladaEn();
    motivoCancelacion = solicitud.motivoCancelacion();
    actualizadaEn = solicitud.actualizadaEn();
  }

  public Solicitud toDomain() {
    AceptacionCondiciones aceptacion = consentimientoVerificado
        ? new AceptacionCondiciones(condicionesSnapshotId, condicionesSnapshotHash, condicionesAceptadasEn)
        : null;
    return Solicitud.rehidratar(id, publicacionId, prestatarioUsuarioId, prestamistaUsuarioId,
        new PeriodoPrestamo(desde, hasta), condiciones(), aceptacion, estado, creadaEn,
        respondidaEn, motivoRechazo, canceladaEn, motivoCancelacion, actualizadaEn,
        aggregateVersion);
  }

  private CondicionesAcordadas condiciones() {
    return new CondicionesAcordadas(tarifaDiariaAceptada, garantiaMonetariaAceptada,
        monedaAceptada, lugarIntercambioAceptado, condicionesUsoAceptadas,
        condicionesEntregaAceptadas, condicionesDevolucionAceptadas,
        condicionesCancelacionAceptadas);
  }

  private static byte[] duplicateKey(Solicitud solicitud) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      digest.update(uuidBytes(solicitud.publicacionId()));
      digest.update(uuidBytes(solicitud.prestatarioId()));
      digest.update(solicitud.periodo().desde().toString().getBytes(StandardCharsets.UTF_8));
      digest.update((byte) 0);
      digest.update(solicitud.periodo().hasta().toString().getBytes(StandardCharsets.UTF_8));
      return digest.digest();
    } catch (Exception exception) {
      throw new IllegalStateException("No se pudo construir la clave de idempotencia", exception);
    }
  }

  private static byte[] uuidBytes(UUID value) {
    return ByteBuffer.allocate(16).putLong(value.getMostSignificantBits())
        .putLong(value.getLeastSignificantBits()).array();
  }

  public UUID getId() { return id; }
  public EstadoSolicitud getEstado() { return estado; }
}
