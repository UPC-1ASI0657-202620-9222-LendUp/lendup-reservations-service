package com.lendup.reservas.config.security;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;

@Configuration
public class SecurityConfiguration {
  private static final String[] SWAGGER_PATHS = {"/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**"};

  @Bean
  @Order(1)
  SecurityFilterChain internalSecurity(HttpSecurity http,
      @Value("${RESERVATIONS_EVIDENCE_SERVICE_TOKEN:}") String token) throws Exception {
    return http.securityMatcher("/internal/**")
        .httpBasic(AbstractHttpConfigurer::disable).formLogin(AbstractHttpConfigurer::disable)
        .logout(AbstractHttpConfigurer::disable).csrf(AbstractHttpConfigurer::disable)
        .requestCache(AbstractHttpConfigurer::disable)
        .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .exceptionHandling(errors -> errors.accessDeniedHandler((request, response, exception) ->
            writeError(response, 403, "NO_AUTORIZADO", "No autorizado")))
        .authorizeHttpRequests(authorize -> authorize
            .requestMatchers(HttpMethod.GET, "/internal/v1/reservas/*/acuerdo").hasAuthority(EvidenceServiceTokenFilter.READ_AGREEMENT)
            .anyRequest().denyAll())
        .addFilterBefore(new EvidenceServiceTokenFilter(token), UsernamePasswordAuthenticationFilter.class)
        .build();
  }

  @Bean
  @Order(2)
  SecurityFilterChain security(
      HttpSecurity http,
      TokenVerifier verifier,
      Environment environment,
      @Value("${lendup.security.cors-origins:}") String origins,
      @Value("${lendup.security.swagger-public:false}") boolean swaggerPublic) throws Exception {
    return http
        .httpBasic(AbstractHttpConfigurer::disable)
        .formLogin(AbstractHttpConfigurer::disable)
        .logout(AbstractHttpConfigurer::disable)
        .requestCache(AbstractHttpConfigurer::disable)
        .csrf(AbstractHttpConfigurer::disable)
        .cors(cors -> cors.configurationSource(request -> corsConfiguration(origins)))
        .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .exceptionHandling(errors -> errors
            .authenticationEntryPoint((request, response, exception) -> writeError(response, 401,
                "AUTENTICACION_REQUERIDA", "Autenticación requerida"))
            .accessDeniedHandler((request, response, exception) -> writeError(response, 403,
                "NO_AUTORIZADO", "No autorizado")))
        .authorizeHttpRequests(authorize -> {
          authorize.requestMatchers(
              "/actuator/health/liveness",
              "/actuator/health/readiness").permitAll();
          if (isSwaggerPublic(swaggerPublic, environment)) authorize.requestMatchers(SWAGGER_PATHS).permitAll();
          else authorize.requestMatchers(SWAGGER_PATHS).authenticated();
          authorize.requestMatchers("/actuator/**").denyAll();
          authorize.anyRequest().authenticated();
        })
        .addFilterBefore(new FirebaseAuthenticationFilter(verifier), UsernamePasswordAuthenticationFilter.class)
        .build();
  }

  static boolean isSwaggerPublic(boolean requested, Environment environment) {
    return requested && !Arrays.asList(environment.getActiveProfiles()).contains("prod");
  }

  private CorsConfiguration corsConfiguration(String configuredOrigins) {
    List<String> origins = Arrays.stream(configuredOrigins.split(","))
        .map(String::trim)
        .filter(value -> !value.isBlank())
        .toList();
    CorsConfiguration configuration = new CorsConfiguration();
    configuration.setAllowedOrigins(origins);
    configuration.setAllowedMethods(List.of("GET", "POST", "OPTIONS"));
    configuration.setAllowedHeaders(List.of("Authorization", "Content-Type", "X-Correlation-ID"));
    configuration.setExposedHeaders(List.of("X-Correlation-ID"));
    configuration.setAllowCredentials(false);
    configuration.setMaxAge(3600L);
    return configuration;
  }

  static void writeError(jakarta.servlet.http.HttpServletResponse response, int status, String code, String message)
      throws java.io.IOException {
    response.setStatus(status);
    response.setContentType("application/json");
    response.setCharacterEncoding(StandardCharsets.UTF_8.name());
    response.getWriter().write("{\"status\":" + status + ",\"error\":\"" + code
        + "\",\"message\":\"" + message + "\"}");
  }
}
