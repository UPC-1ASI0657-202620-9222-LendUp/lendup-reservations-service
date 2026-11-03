package com.lendup.reservas.infrastructure.client;

import com.lendup.reservas.application.exception.DependencyUnavailableException;
import com.lendup.reservas.application.port.out.PrestamoAccessPort;
import com.lendup.reservas.config.IntegrationProperties;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/** Consume el contrato interno propuesto; si no está configurado, falla de forma segura. */
@Component
public class PrestamoAccessRestAdapter implements PrestamoAccessPort {
  private final IntegrationProperties.Loan config;
  private final RestClient client;

  public PrestamoAccessRestAdapter(IntegrationProperties properties) {
    config = properties.loan();
    client = config.baseUrl().isBlank() ? null
        : RestClientFactory.create(config.baseUrl(), config.connectTimeoutMs(),
            config.readTimeoutMs());
  }

  @Override
  @SuppressWarnings("unchecked")
  public boolean contactoVigente(UUID reservaId, UUID participanteId) {
    if (client == null || config.serviceToken().isBlank()
        || config.contactAccessUriTemplate().isBlank()) {
      throw new DependencyUnavailableException(
          "Integración de vigencia de contacto con Préstamos no configurada");
    }
    try {
      Map<String, Object> row = client.get()
          .uri(config.contactAccessUriTemplate(), reservaId, participanteId)
          .header("Authorization", "Bearer " + config.serviceToken())
          .retrieve().body(Map.class);
      if (row == null || !(row.get("contacto_vigente") instanceof Boolean value)) {
        throw new DependencyUnavailableException("Préstamos no confirmó la vigencia del contacto");
      }
      return value;
    } catch (DependencyUnavailableException exception) {
      throw exception;
    } catch (Exception exception) {
      throw new DependencyUnavailableException("Préstamos no disponible", exception);
    }
  }
}
