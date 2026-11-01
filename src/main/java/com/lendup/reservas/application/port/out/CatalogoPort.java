package com.lendup.reservas.application.port.out;

import com.lendup.reservas.domain.model.CondicionesAcordadas;
import com.lendup.reservas.domain.model.PeriodoPrestamo;
import java.util.UUID;

public interface CatalogoPort {
  PublicacionSnapshot obtenerSnapshot(UUID publicacionId, PeriodoPrestamo periodo);
  record PublicacionSnapshot(UUID id, UUID propietarioId, boolean activa, boolean periodoOfrecido, CondicionesAcordadas condiciones) {}
}
