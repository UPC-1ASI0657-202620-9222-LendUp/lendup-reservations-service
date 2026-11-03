package com.lendup.reservas.interfaces.rest.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.time.LocalDateTime;
import java.util.UUID;

public final class RequestDtos {
  private RequestDtos() {}

  public record AceptacionCondicionesRequest(
      @NotBlank @Size(max = 100) String snapshot_id,
      @NotBlank @Pattern(regexp = "[0-9a-fA-F]{64}") String snapshot_hash,
      @AssertTrue(message = "Debe aceptar explícitamente las condiciones") boolean aceptada) {}

  public record CrearSolicitud(
      @NotNull UUID publicacion_id,
      @NotNull LocalDateTime desde,
      @NotNull LocalDateTime hasta,
      @NotNull @Valid AceptacionCondicionesRequest aceptacion_condiciones) {}

  public record RechazarSolicitud(@NotBlank @Size(max = 1000) String motivo_rechazo) {}
  public record CancelarSolicitud(@NotBlank @Size(max = 1000) String motivo_cancelacion) {}
}
