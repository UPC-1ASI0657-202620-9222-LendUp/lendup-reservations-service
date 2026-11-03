package com.lendup.reservas.application.port.out;

import java.util.UUID;

/** Autoridad de Préstamos para decidir si el contacto sigue siendo visible. */
public interface PrestamoAccessPort {
  boolean contactoVigente(UUID reservaId, UUID participanteId);
}
