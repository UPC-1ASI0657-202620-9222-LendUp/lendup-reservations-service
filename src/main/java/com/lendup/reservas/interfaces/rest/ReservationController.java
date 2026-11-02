package com.lendup.reservas.interfaces.rest;

import com.lendup.reservas.application.port.in.ReservationUseCases;
import com.lendup.reservas.config.security.ActorContext;
import com.lendup.reservas.interfaces.rest.dto.RequestDtos.*;
import com.lendup.reservas.interfaces.rest.dto.ResponseDtos.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import java.util.*;
import org.slf4j.MDC;
import org.springframework.http.*;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Validated @RestController @RequestMapping("/api/v1") @Tag(name="Reservas")
public class ReservationController {
  private final ReservationUseCases useCases; private final ActorContext actor;
  public ReservationController(ReservationUseCases u,ActorContext a){useCases=u;actor=a;}
  @PostMapping("/solicitudes") @Operation(operationId="crearSolicitud",summary="Crear solicitud",responses={@ApiResponse(responseCode="201",description="Solicitud creada"),@ApiResponse(responseCode="400",description="Solicitud inválida"),@ApiResponse(responseCode="409",description="Conflicto de negocio"),@ApiResponse(responseCode="503",description="Dependencia no disponible")})
  ResponseEntity<SolicitudResponse> create(@Valid @RequestBody CrearSolicitud b){return ResponseEntity.status(HttpStatus.CREATED).body(SolicitudResponse.from(useCases.crearSolicitud(actor.userId(),b.publicacion_id(),b.desde(),b.hasta())));}
  @GetMapping("/solicitudes") @Operation(operationId="listarSolicitudes",summary="Listar solicitudes") List<SolicitudResponse> requests(@RequestParam(required=false) @Pattern(regexp="PRESTAMISTA|PRESTATARIO") String rol,@RequestParam(required=false) @Pattern(regexp="PENDIENTE|ACEPTADA|RECHAZADA|CANCELADA") String estado){return useCases.listarSolicitudes(actor.userId(),actor.administrator(),rol,estado).stream().map(SolicitudResponse::from).toList();}
  @GetMapping("/solicitudes/{id}") @Operation(operationId="obtenerSolicitud",summary="Obtener solicitud") SolicitudResponse request(@PathVariable UUID id){return SolicitudResponse.from(useCases.obtenerSolicitud(actor.userId(),actor.administrator(),id));}
  @PostMapping("/solicitudes/{id}/aceptacion") @Operation(operationId="aceptarSolicitud",summary="Aceptar solicitud",responses={@ApiResponse(responseCode="201",description="Reserva confirmada"),@ApiResponse(responseCode="409",description="Estado o periodo incompatible")}) ResponseEntity<ReservaResponse> accept(@PathVariable UUID id){return ResponseEntity.status(HttpStatus.CREATED).body(ReservaResponse.from(useCases.aceptarSolicitud(actor.userId(),id)));}
  @PostMapping("/solicitudes/{id}/rechazo") @Operation(operationId="rechazarSolicitud",summary="Rechazar solicitud") SolicitudResponse reject(@PathVariable UUID id,@Valid @RequestBody RechazarSolicitud b){return SolicitudResponse.from(useCases.rechazarSolicitud(actor.userId(),id,b.motivo_rechazo()));}
  @PostMapping("/solicitudes/{id}/cancelacion") @Operation(operationId="cancelarSolicitud",summary="Cancelar solicitud") SolicitudResponse cancelRequest(@PathVariable UUID id,@Valid @RequestBody CancelarSolicitud b){return SolicitudResponse.from(useCases.cancelarSolicitud(actor.userId(),id,b.motivo_cancelacion()));}
  @GetMapping("/reservas") @Operation(operationId="listarReservas",summary="Listar reservas") List<ReservaResponse> reservations(@RequestParam(required=false) @Pattern(regexp="PRESTAMISTA|PRESTATARIO") String rol){return useCases.listarReservas(actor.userId(),actor.administrator(),rol).stream().map(ReservaResponse::from).toList();}
  @PostMapping("/reservas/{id}/cancelacion") @Operation(operationId="solicitarCancelacionReserva",summary="Solicitar cancelación de reserva") ReservaResponse cancelReservation(@PathVariable UUID id,@Valid @RequestBody CancelarSolicitud b){return ReservaResponse.from(useCases.solicitarCancelacionReserva(actor.userId(),actor.administrator(),id,b.motivo_cancelacion(),MDC.get("correlation_id")));}
  @GetMapping("/reservas/{id}/contacto") @Operation(operationId="obtenerContacto",summary="Obtener contacto autorizado") ContactoResponse contact(@PathVariable UUID id){return new ContactoResponse(useCases.obtenerContacto(actor.userId(),actor.administrator(),id).telefono());}
}
