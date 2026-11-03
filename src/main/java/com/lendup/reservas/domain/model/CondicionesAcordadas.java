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
  private static final int MAX_LUGAR_LENGTH = 250;
  private static final int MAX_CONDICION_LENGTH = 16_000;

  public CondicionesAcordadas {
    validarImporte(tarifaDiaria, false, "Tarifa diaria inválida");
    validarImporte(garantiaMonetaria, true, "Garantía inválida");
    moneda = normalizarMoneda(moneda);
    lugarIntercambio = textoObligatorio(lugarIntercambio, MAX_LUGAR_LENGTH, "Lugar de intercambio inválido");
    uso = textoObligatorio(uso, MAX_CONDICION_LENGTH, "Condiciones de uso inválidas");
    entrega = textoObligatorio(entrega, MAX_CONDICION_LENGTH, "Condiciones de entrega inválidas");
    devolucion = textoObligatorio(devolucion, MAX_CONDICION_LENGTH, "Condiciones de devolución inválidas");
    cancelacion = textoObligatorio(cancelacion, MAX_CONDICION_LENGTH, "Condiciones de cancelación inválidas");
  }

  private static void validarImporte(BigDecimal value, boolean nullable, String message) {
    if (value == null) {
      if (nullable) return;
      throw new IllegalArgumentException(message);
    }
    if ((!nullable && value.signum() <= 0) || (nullable && value.signum() < 0)
        || value.scale() > 2 || value.precision() - value.scale() > 10) {
      throw new IllegalArgumentException(message);
    }
  }

  private static String normalizarMoneda(String value) {
    if (value == null) throw new IllegalArgumentException("Moneda inválida");
    String normalized = value.trim();
    if (!normalized.matches("[A-Z]{3}")) throw new IllegalArgumentException("Moneda inválida");
    return normalized;
  }

  private static String textoObligatorio(String value, int maxLength, String message) {
    if (value == null) throw new IllegalArgumentException(message);
    String normalized = value.trim();
    if (normalized.isEmpty() || normalized.length() > maxLength) throw new IllegalArgumentException(message);
    return normalized;
  }
}
