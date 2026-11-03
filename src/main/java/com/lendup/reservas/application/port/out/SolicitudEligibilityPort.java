package com.lendup.reservas.application.port.out;

import com.lendup.reservas.domain.specification.SolicitudSpecifications.EstadoComprobacion;
import java.util.UUID;

/** Boundary for authoritative eligibility facts owned by Identidad. */
public interface SolicitudEligibilityPort {
  EligibilityFacts obtain(UUID usuarioId);

  record EligibilityFacts(
      EstadoComprobacion verificacionInstitucional,
      EstadoComprobacion terminosGenerales) {}
}
