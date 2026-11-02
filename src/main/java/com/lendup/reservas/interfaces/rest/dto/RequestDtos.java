package com.lendup.reservas.interfaces.rest.dto;
import jakarta.validation.constraints.*; import java.time.LocalDateTime; import java.util.UUID;
public final class RequestDtos { private RequestDtos(){} public record CrearSolicitud(@NotNull UUID publicacion_id,@NotNull LocalDateTime desde,@NotNull LocalDateTime hasta){} public record RechazarSolicitud(@NotBlank @Size(max=1000) String motivo_rechazo){} public record CancelarSolicitud(@NotBlank @Size(max=1000) String motivo_cancelacion){} }
