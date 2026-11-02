package com.lendup.reservas.application.port.out;

import com.lendup.reservas.domain.specification.SolicitudSpecifications.EstadoComprobacion;
import java.util.UUID;

/**
 * Proposed boundary for eligibility facts that Identidad must own. It is intentionally not wired
 * until that team approves an authenticated contract; unknown facts must never become verified.
 */
public interface SolicitudEligibilityPort {
  EligibilityFacts obtain(UUID usuarioId);

  record EligibilityFacts(
      EstadoComprobacion verificacionInstitucional,
      EstadoComprobacion terminosGenerales) {}
}
