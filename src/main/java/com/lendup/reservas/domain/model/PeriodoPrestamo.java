package com.lendup.reservas.domain.model;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;

public record PeriodoPrestamo(LocalDateTime desde, LocalDateTime hasta) {
  public PeriodoPrestamo {
    if (desde == null || hasta == null || !desde.isBefore(hasta)) {
      throw new IllegalArgumentException("El periodo requiere desde < hasta");
    }
  }

  public boolean seSuperponeCon(PeriodoPrestamo otro) {
    return desde.isBefore(otro.hasta) && hasta.isAfter(otro.desde);
  }

  public long bloquesIniciadosDe24Horas() {
    Duration duration = Duration.between(desde, hasta);
    long fullBlocks = duration.getSeconds() / 86_400L;
    boolean hasStartedBlock = duration.getSeconds() % 86_400L != 0 || duration.getNano() != 0;
    return hasStartedBlock ? Math.incrementExact(fullBlocks) : fullBlocks;
  }

  public BigDecimal importe(BigDecimal tarifaDiaria) {
    if (tarifaDiaria == null || tarifaDiaria.signum() <= 0) {
      throw new IllegalArgumentException("La tarifa diaria debe ser positiva");
    }
    return tarifaDiaria.multiply(BigDecimal.valueOf(bloquesIniciadosDe24Horas()));
  }
}
