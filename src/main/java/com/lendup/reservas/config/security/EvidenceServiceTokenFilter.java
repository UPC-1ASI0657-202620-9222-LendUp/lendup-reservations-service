package com.lendup.reservas.config.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

final class EvidenceServiceTokenFilter extends OncePerRequestFilter {
  static final String READ_AGREEMENT = "RESERVATIONS_READ_AGREEMENT";
  private final byte[] expected;

  EvidenceServiceTokenFilter(String token) {
    expected = token.matches("[A-Za-z0-9_-]{32,}") ? token.getBytes(StandardCharsets.UTF_8) : null;
  }

  @Override
  protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws ServletException, IOException {
    String header = request.getHeader("Authorization");
    if (expected == null || header == null || !header.startsWith("Bearer ")
        || !MessageDigest.isEqual(expected, header.substring(7).getBytes(StandardCharsets.UTF_8))) {
      SecurityConfiguration.writeError(response, 401, "CREDENCIAL_DE_SERVICIO_INVALIDA", "Credencial de servicio inválida");
      return;
    }
    SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
        "evidence-service", null, List.of(new SimpleGrantedAuthority(READ_AGREEMENT))));
    chain.doFilter(request, response);
  }
}
