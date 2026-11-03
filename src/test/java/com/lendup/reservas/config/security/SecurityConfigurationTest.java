package com.lendup.reservas.config.security;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

class SecurityConfigurationTest {
  @Test void swaggerPublicoNoPuedeHabilitarseEnPerfilProduccion() {
    MockEnvironment production=new MockEnvironment(); production.setActiveProfiles("prod");
    assertFalse(SecurityConfiguration.isSwaggerPublic(true,production));
    MockEnvironment development=new MockEnvironment(); development.setActiveProfiles("dev");
    assertTrue(SecurityConfiguration.isSwaggerPublic(true,development));
  }

  @Test void valoresPorDefectoMantienenSpringdocYSwaggerPublicoDeshabilitados() throws Exception {
    String configuration = java.nio.file.Files.readString(java.nio.file.Path.of("src/main/resources/application.yml"));
    assertTrue(configuration.contains("api-docs.enabled: ${SPRINGDOC_ENABLED:false}"));
    assertTrue(configuration.contains("swagger-public: ${SWAGGER_PUBLIC:false}"));
  }
}
