package com.lendup.reservas.infrastructure.client;

import com.lendup.reservas.application.exception.DependencyUnavailableException;
import com.lendup.reservas.application.port.out.IdentidadPort;
import com.lendup.reservas.application.port.out.SolicitudEligibilityPort;
import com.lendup.reservas.config.IntegrationProperties;
import com.lendup.reservas.domain.specification.SolicitudSpecifications.EstadoComprobacion;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class IdentidadRestAdapter implements IdentidadPort, SolicitudEligibilityPort {
  private final IntegrationProperties.Identity config;
  private final RestClient client;

  public IdentidadRestAdapter(IntegrationProperties properties) {
    config = properties.identity();
    client = config.baseUrl().isBlank() ? null
        : RestClientFactory.create(config.baseUrl(), config.connectTimeoutMs(),
            config.readTimeoutMs());
  }

  @Override public UUID resolverUsuarioId(String firebaseUid) {
    requireConfigured(config.firebaseUserUriTemplate(), "resolución de identidad");
    Map<String, Object> row = get(config.firebaseUserUriTemplate(), firebaseUid);
    if (row.get("usuario_id") == null) {
      throw new DependencyUnavailableException("Identidad no vinculó el token a un usuario interno");
    }
    try {
      return UUID.fromString(row.get("usuario_id").toString());
    } catch (IllegalArgumentException exception) {
      throw new DependencyUnavailableException("Identidad devolvió un usuario inválido", exception);
    }
  }

  @Override public String obtenerTelefonoAutorizado(UUID usuarioId, UUID reservaId) {
    requireConfigured(config.authorizedContactUriTemplate(), "contacto autorizado");
    Map<String, Object> row = get(config.authorizedContactUriTemplate(), usuarioId, reservaId);
    Object telefono = row.get("telefono");
    if (telefono == null || telefono.toString().isBlank()) {
      throw new DependencyUnavailableException("Identidad no entregó teléfono autorizado");
    }
    return telefono.toString();
  }

  @Override public EligibilityFacts obtain(UUID usuarioId) {
    requireConfigured(config.eligibilityUriTemplate(), "elegibilidad de solicitudes");
    Map<String, Object> row = get(config.eligibilityUriTemplate(), usuarioId);
    return new EligibilityFacts(verificationState(row.get("estado_verificacion")),
        booleanState(row.get("terminos_generales_vigentes_aceptados")));
  }

  @SuppressWarnings("unchecked")
  private Map<String, Object> get(String uriTemplate, Object... variables) {
    try {
      Map<String, Object> row = client.get().uri(uriTemplate, variables)
          .header("Authorization", "Bearer " + config.serviceToken())
          .retrieve().body(Map.class);
      if (row == null) throw new DependencyUnavailableException("Identidad devolvió una respuesta vacía");
      return row;
    } catch (DependencyUnavailableException exception) {
      throw exception;
    } catch (Exception exception) {
      throw new DependencyUnavailableException("Identidad no disponible", exception);
    }
  }

  private EstadoComprobacion verificationState(Object value) {
    if (value == null) return EstadoComprobacion.DESCONOCIDO;
    return switch (value.toString()) {
      case "VERIFICADO" -> EstadoComprobacion.CUMPLE;
      case "NO_VERIFICADO", "PENDIENTE", "RECHAZADO" -> EstadoComprobacion.NO_CUMPLE;
      default -> EstadoComprobacion.DESCONOCIDO;
    };
  }

  private EstadoComprobacion booleanState(Object value) {
    if (value instanceof Boolean flag) {
      return flag ? EstadoComprobacion.CUMPLE : EstadoComprobacion.NO_CUMPLE;
    }
    return EstadoComprobacion.DESCONOCIDO;
  }

  private void requireConfigured(String uriTemplate, String operation) {
    if (client == null || config.serviceToken().isBlank() || uriTemplate.isBlank()) {
      throw new DependencyUnavailableException(
          "Integración de " + operation + " con Identidad no configurada");
    }
  }
}
