package com.lendup.reservas.domain.model;

import java.util.UUID;
import java.util.Objects;

public final class Reserva {
  private final UUID id;
  private final UUID solicitudId;
  private final UUID agendaId;
  private final PeriodoPrestamo periodo;
  private final CondicionesAcordadas condiciones;
  private EstadoReserva estado;
  public Reserva(UUID id, UUID solicitudId, UUID agendaId, PeriodoPrestamo periodo, CondicionesAcordadas condiciones, EstadoReserva estado) {
    this.id=Objects.requireNonNull(id,"El ID de reserva es obligatorio");
    this.solicitudId=Objects.requireNonNull(solicitudId,"La solicitud es obligatoria");
    this.agendaId=Objects.requireNonNull(agendaId,"La agenda es obligatoria");
    this.periodo=Objects.requireNonNull(periodo,"El periodo es obligatorio");
    this.condiciones=Objects.requireNonNull(condiciones,"Las condiciones son obligatorias");
    this.estado=Objects.requireNonNull(estado,"El estado es obligatorio");
  }
  public void solicitarCancelacion(){ if(estado!=EstadoReserva.CONFIRMADA) throw new IllegalStateException("Reserva no confirmada"); estado=EstadoReserva.CANCELACION_PENDIENTE; }
  public void confirmarCancelacion(){ if(estado!=EstadoReserva.CANCELACION_PENDIENTE) throw new IllegalStateException("Cancelación no pendiente"); estado=EstadoReserva.CANCELADA; }
  public void rechazarCancelacion(){ if(estado!=EstadoReserva.CANCELACION_PENDIENTE) throw new IllegalStateException("Cancelación no pendiente"); estado=EstadoReserva.CONFIRMADA; }
  public UUID id(){return id;} public UUID solicitudId(){return solicitudId;} public UUID agendaId(){return agendaId;} public PeriodoPrestamo periodo(){return periodo;}
  public CondicionesAcordadas condiciones(){return condiciones;} public EstadoReserva estado(){return estado;}
}
