package com.lendup.reservas.domain.model;

import java.math.BigDecimal;

public record CondicionesAcordadas(
    BigDecimal tarifaDiaria,
    BigDecimal garantiaMonetaria,
    String moneda,
    String lugarIntercambio,
    String uso,
    String entrega,
    String devolucion,
    String cancelacion) {
  public CondicionesAcordadas {
    if (tarifaDiaria == null || tarifaDiaria.signum() <= 0) throw new IllegalArgumentException("Tarifa diaria inválida");
    if (garantiaMonetaria != null && garantiaMonetaria.signum() < 0) throw new IllegalArgumentException("Garantía inválida");
    if (moneda == null || !moneda.matches("[A-Z]{3}")) throw new IllegalArgumentException("Moneda inválida");
    if (lugarIntercambio == null || lugarIntercambio.isBlank()) throw new IllegalArgumentException("Lugar requerido");
    if (uso == null || entrega == null || devolucion == null || cancelacion == null) throw new IllegalArgumentException("Condiciones incompletas");
  }
}
