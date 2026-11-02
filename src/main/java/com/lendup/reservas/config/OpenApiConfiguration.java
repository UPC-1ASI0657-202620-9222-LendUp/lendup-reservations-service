package com.lendup.reservas.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfiguration {
  public static final String FIREBASE_BEARER = "firebaseBearer";

  @Bean
  OpenAPI reservationsOpenApi() {
    SecurityScheme firebaseBearer = new SecurityScheme()
        .type(SecurityScheme.Type.HTTP)
        .scheme("bearer")
        .bearerFormat("JWT")
        .description("Firebase ID Token verificado por Firebase Admin SDK");
    return new OpenAPI()
        .info(new Info().title("LendUp Reservations API").version("v1"))
        .components(new Components().addSecuritySchemes(FIREBASE_BEARER, firebaseBearer))
        .addSecurityItem(new SecurityRequirement().addList(FIREBASE_BEARER));
  }
}
