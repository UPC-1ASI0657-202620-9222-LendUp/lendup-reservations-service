package com.lendup.reservas.application.port.in;

import com.lendup.reservas.application.port.in.ReservationModels.*;
import java.util.List;
import java.util.UUID;

public interface ReservationUseCases {
  SolicitudView crearSolicitud(CrearSolicitudCommand command);
  List<SolicitudView> listarSolicitudes(UUID actor, boolean admin, String rol, String estado);
  SolicitudView obtenerSolicitud(UUID actor, boolean admin, UUID id);
  ReservaView aceptarSolicitud(UUID actor, UUID id);
  SolicitudView rechazarSolicitud(UUID actor, UUID id, String motivo);
  SolicitudView cancelarSolicitud(UUID actor, UUID id, String motivo);
  List<ReservaView> listarReservas(UUID actor, boolean admin, String rol);
  ReservaView solicitarCancelacionReserva(UUID actor, boolean admin, UUID id, String motivo,
      String correlationId);
  ContactoView obtenerContacto(UUID actor, boolean admin, UUID id);
}
