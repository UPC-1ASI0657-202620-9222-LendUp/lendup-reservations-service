package com.lendup.reservas.application.port.in;

import com.lendup.reservas.domain.model.AceptacionCondiciones;
import com.lendup.reservas.domain.model.CondicionesAcordadas;
import com.lendup.reservas.domain.model.Reserva;
import com.lendup.reservas.domain.model.Solicitud;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

public final class ReservationModels {
  private ReservationModels() {}

  public record CrearSolicitudCommand(UUID actorId, boolean correoFirebaseVerificado,
      UUID publicacionId, LocalDateTime desde, LocalDateTime hasta, String snapshotId,
      String snapshotHash, boolean aceptacionExplicita) {}

  public record SolicitudView(UUID id, UUID publicacionId, UUID prestatarioUsuarioId,
      UUID prestamistaUsuarioId, LocalDateTime desde, LocalDateTime hasta, String estado,
      BigDecimal tarifaDiariaAceptada, BigDecimal garantiaMonetariaAceptada,
      String monedaAceptada, String lugarIntercambioAceptado, String condicionesUsoAceptadas,
      String condicionesEntregaAceptadas, String condicionesDevolucionAceptadas,
      String condicionesCancelacionAceptadas, String condicionesSnapshotId,
      String condicionesSnapshotHash, Instant condicionesAceptadasEn, Instant creadaEn,
      Instant respondidaEn, String motivoRechazo, Instant canceladaEn,
      String motivoCancelacion, Instant actualizadaEn) {
    public static SolicitudView from(Solicitud solicitud) {
      CondicionesAcordadas c = solicitud.condiciones();
      AceptacionCondiciones a = solicitud.aceptacionCondiciones();
      return new SolicitudView(solicitud.id(), solicitud.publicacionId(),
          solicitud.prestatarioId(), solicitud.prestamistaId(), solicitud.periodo().desde(),
          solicitud.periodo().hasta(), solicitud.estado().name(), c.tarifaDiaria(),
          c.garantiaMonetaria(), c.moneda(), c.lugarIntercambio(), c.uso(), c.entrega(),
          c.devolucion(), c.cancelacion(), a == null ? null : a.snapshotId(),
          a == null ? null : a.snapshotHash(), a == null ? null : a.aceptadaEn(),
          solicitud.creadaEn(), solicitud.respondidaEn(), solicitud.motivoRechazo(),
          solicitud.canceladaEn(), solicitud.motivoCancelacion(), solicitud.actualizadaEn());
    }
  }

  public record ReservaView(UUID id, UUID solicitudId, UUID agendaId, LocalDateTime desde,
      LocalDateTime hasta, String estado, BigDecimal tarifaDiariaAcordada,
      BigDecimal garantiaMonetariaAcordada, String monedaAcordada,
      String lugarIntercambioAcordado, String condicionesUsoAcordadas,
      String condicionesEntregaAcordadas, String condicionesDevolucionAcordadas,
      String condicionesCancelacionAcordadas, String condicionesSnapshotId,
      String condicionesSnapshotHash, Instant condicionesAceptadasEn,
      LocalDateTime ocupacionHasta, LocalDateTime periodoLiberadoDesde,
      Instant confirmadaEn, Instant cancelacionSolicitadaEn, Instant canceladaEn,
      UUID canceladaPorUsuarioId, String motivoCancelacion, Instant actualizadaEn) {
    public static ReservaView from(Reserva reserva) {
      CondicionesAcordadas c = reserva.condiciones();
      AceptacionCondiciones a = reserva.aceptacionCondiciones();
      return new ReservaView(reserva.id(), reserva.solicitudId(), reserva.agendaId(),
          reserva.periodoContractual().desde(), reserva.periodoContractual().hasta(),
          reserva.estado().name(), c.tarifaDiaria(), c.garantiaMonetaria(), c.moneda(),
          c.lugarIntercambio(), c.uso(), c.entrega(), c.devolucion(), c.cancelacion(),
          a == null ? null : a.snapshotId(), a == null ? null : a.snapshotHash(),
          a == null ? null : a.aceptadaEn(), reserva.ocupacionHasta(),
          reserva.periodoLiberadoDesde(), reserva.confirmadaEn(),
          reserva.cancelacionSolicitadaEn(), reserva.canceladaEn(),
          reserva.canceladaPorUsuarioId(), reserva.motivoCancelacion(), reserva.actualizadaEn());
    }
  }

  public record ContactoView(String telefono) {}
}
