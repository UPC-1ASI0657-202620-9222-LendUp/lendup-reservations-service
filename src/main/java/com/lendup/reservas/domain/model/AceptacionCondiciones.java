package com.lendup.reservas.domain.model;

import java.time.Instant;
import java.util.Locale;
import java.util.Objects;

/** Evidencia inmutable de que un usuario aceptó un snapshot concreto. */
public record AceptacionCondiciones(String snapshotId, String snapshotHash, Instant aceptadaEn) {
  public AceptacionCondiciones {
    snapshotId = requireText(snapshotId, "La versión del snapshot es obligatoria", 100);
    snapshotHash = requireText(snapshotHash, "El hash del snapshot es obligatorio", 64)
        .toLowerCase(Locale.ROOT);
    if (!snapshotHash.matches("[0-9a-f]{64}")) {
      throw new IllegalArgumentException("El hash del snapshot debe ser SHA-256 hexadecimal");
    }
    Objects.requireNonNull(aceptadaEn, "La fecha de aceptación es obligatoria");
  }

  private static String requireText(String value, String message, int maxLength) {
    if (value == null || value.isBlank()) throw new IllegalArgumentException(message);
    String normalized = value.trim();
    if (normalized.length() > maxLength) throw new IllegalArgumentException(message);
    return normalized;
  }
}
