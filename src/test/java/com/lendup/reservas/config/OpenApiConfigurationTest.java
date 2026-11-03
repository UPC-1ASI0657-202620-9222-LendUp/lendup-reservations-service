package com.lendup.reservas.config;

import static org.junit.jupiter.api.Assertions.*;

import io.swagger.v3.oas.models.security.SecurityScheme;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class OpenApiConfigurationTest {
  @Test void registraFirebaseBearerYLoAplicaGlobalmente() {
    var openApi = new OpenApiConfiguration().reservationsOpenApi();
    var scheme = openApi.getComponents().getSecuritySchemes().get(OpenApiConfiguration.FIREBASE_BEARER);
    assertAll(
        () -> assertEquals(SecurityScheme.Type.HTTP, scheme.getType()),
        () -> assertEquals("bearer", scheme.getScheme()),
        () -> assertEquals("JWT", scheme.getBearerFormat()),
        () -> assertTrue(openApi.getSecurity().getFirst().containsKey(OpenApiConfiguration.FIREBASE_BEARER)));
  }

  @Test void contratoVersionadoConservaNueveOperacionesYFirebaseBearer() throws Exception {
    String contract = Files.readString(Path.of("contracts/openapi/reservations-v1.yaml"));
    assertTrue(contract.contains("firebaseBearer: { type: http, scheme: bearer, bearerFormat: JWT,"));
    assertEquals(9, contract.split("operationId:", -1).length - 1);
    assertAll(
        () -> assertTrue(contract.contains("required: [publicacion_id, desde, hasta, aceptacion_condiciones]")),
        () -> assertTrue(contract.contains("required: [snapshot_id, snapshot_hash, aceptada]")),
        () -> assertTrue(contract.contains("aceptada: { const: true }")));
  }
}
