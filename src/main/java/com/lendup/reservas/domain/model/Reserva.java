package com.lendup.reservas.domain.model;

import java.util.UUID;

public final class Reserva {
  private final UUID id;
  private final UUID solicitudId;
  private final PeriodoPrestamo periodo;
  private final CondicionesAcordadas condiciones;
  private EstadoReserva estado;
  public Reserva(UUID id, UUID solicitudId, PeriodoPrestamo periodo, CondicionesAcordadas condiciones, EstadoReserva estado) {
    this.id=id; this.solicitudId=solicitudId; this.periodo=periodo; this.condiciones=condiciones; this.estado=estado;
  }
  public void solicitarCancelacion(){ if(estado!=EstadoReserva.CONFIRMADA) throw new IllegalStateException("Reserva no confirmada"); estado=EstadoReserva.CANCELACION_PENDIENTE; }
  public void confirmarCancelacion(){ if(estado!=EstadoReserva.CANCELACION_PENDIENTE) throw new IllegalStateException("Cancelación no pendiente"); estado=EstadoReserva.CANCELADA; }
  public void rechazarCancelacion(){ if(estado!=EstadoReserva.CANCELACION_PENDIENTE) throw new IllegalStateException("Cancelación no pendiente"); estado=EstadoReserva.CONFIRMADA; }
  public UUID id(){return id;} public UUID solicitudId(){return solicitudId;} public PeriodoPrestamo periodo(){return periodo;}
  public CondicionesAcordadas condiciones(){return condiciones;} public EstadoReserva estado(){return estado;}
}
