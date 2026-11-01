package com.lendup.reservas.infrastructure.client;

import static org.junit.jupiter.api.Assertions.assertThrows;

import com.lendup.reservas.application.exception.DependencyUnavailableException;
import com.lendup.reservas.config.IntegrationProperties;
import com.lendup.reservas.domain.model.PeriodoPrestamo;
import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class IntegrationConfigurationTest {
  private final IntegrationProperties properties = new IntegrationProperties(null, null);

  @Test
  void catalogoFallaSeguroSinContratoConfigurado() {
    CatalogoRestAdapter adapter = new CatalogoRestAdapter(properties);
    PeriodoPrestamo period = new PeriodoPrestamo(
        LocalDateTime.of(2026, 11, 2, 10, 0),
        LocalDateTime.of(2026, 11, 3, 10, 0));
    assertThrows(DependencyUnavailableException.class,
        () -> adapter.obtenerSnapshot(UUID.randomUUID(), period));
  }

  @Test
  void identidadFallaSeguroSinContratosConfigurados() {
    IdentidadRestAdapter adapter = new IdentidadRestAdapter(properties);
    assertThrows(DependencyUnavailableException.class,
        () -> adapter.resolverUsuarioId("firebase-uid"));
    assertThrows(DependencyUnavailableException.class,
        () -> adapter.obtenerTelefonoAutorizado(UUID.randomUUID(), UUID.randomUUID()));
  }
}
