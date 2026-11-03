package com.lendup.reservas.domain.model;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

public final class AgendaDelObjeto {
  private final UUID id;
  private final UUID publicacionId;
  private final long version;
  private final List<Bloqueo> bloqueos;

  public AgendaDelObjeto(UUID id, UUID publicacionId, long version, List<Bloqueo> bloqueos) {
    this.id = Objects.requireNonNull(id, "El ID de agenda es obligatorio");
    this.publicacionId = Objects.requireNonNull(publicacionId, "La publicación es obligatoria");
    if (version < 0) throw new IllegalArgumentException("La versión de agenda no puede ser negativa");
    this.version = version;
    this.bloqueos = List.copyOf(Objects.requireNonNull(bloqueos, "Los bloqueos son obligatorios"));
  }

  public void exigirDisponible(PeriodoPrestamo candidato) {
    Objects.requireNonNull(candidato, "El periodo candidato es obligatorio");
    boolean overlapping = bloqueos.stream()
        .filter(Bloqueo::mantienePeriodoBloqueado)
        .map(Bloqueo::periodo)
        .anyMatch(candidato::seSuperponeCon);
    if (overlapping) throw new IllegalStateException("Periodo ya reservado");
  }

  public boolean estaDisponible(PeriodoPrestamo candidato) {
    try {
      exigirDisponible(candidato);
      return true;
    } catch (IllegalStateException exception) {
      return false;
    }
  }

  public UUID id() { return id; }
  public UUID publicacionId() { return publicacionId; }
  public long version() { return version; }
  public List<Bloqueo> bloqueos() { return bloqueos; }

  public record Bloqueo(UUID reservaId, PeriodoPrestamo periodo, EstadoReserva estado) {
    public Bloqueo {
      Objects.requireNonNull(reservaId, "La reserva es obligatoria");
      Objects.requireNonNull(periodo, "El periodo es obligatorio");
      Objects.requireNonNull(estado, "El estado es obligatorio");
    }

    public boolean mantienePeriodoBloqueado() {
      return estado == EstadoReserva.CONFIRMADA || estado == EstadoReserva.CANCELACION_PENDIENTE;
    }
  }
}
