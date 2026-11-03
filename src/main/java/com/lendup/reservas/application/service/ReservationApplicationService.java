package com.lendup.reservas.application.service;

import com.lendup.reservas.application.exception.*;
import com.lendup.reservas.application.port.in.ReservationModels.*;
import com.lendup.reservas.application.port.in.ReservationUseCases;
import com.lendup.reservas.application.port.out.*;
import com.lendup.reservas.domain.event.EventEnvelope;
import com.lendup.reservas.domain.model.*;
import com.lendup.reservas.domain.specification.SolicitudSpecifications;
import com.lendup.reservas.domain.specification.SolicitudSpecifications.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.*;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReservationApplicationService implements ReservationUseCases {
  private final ReservationPersistencePort persistence;
  private final CatalogoPort catalogo;
  private final IdentidadPort identidad;
  private final SolicitudEligibilityPort eligibility;
  private final PrestamoAccessPort prestamoAccess;
  private final OutboxPort outbox;
  private final AuditPort audit;
  private final CorrelationIdPort correlation;
  private final Clock clock;
  private final ReservaFactory reservaFactory;

  public ReservationApplicationService(ReservationPersistencePort persistence,
      CatalogoPort catalogo, IdentidadPort identidad, SolicitudEligibilityPort eligibility,
      PrestamoAccessPort prestamoAccess, OutboxPort outbox, AuditPort audit, Clock clock,
      ReservaFactory reservaFactory, CorrelationIdPort correlation) {
    this.persistence = persistence;
    this.catalogo = catalogo;
    this.identidad = identidad;
    this.eligibility = eligibility;
    this.prestamoAccess = prestamoAccess;
    this.outbox = outbox;
    this.audit = audit;
    this.clock = clock;
    this.reservaFactory = reservaFactory;
    this.correlation = correlation;
  }

  @Override
  @Transactional
  public SolicitudView crearSolicitud(CrearSolicitudCommand command) {
    Objects.requireNonNull(command, "El comando es obligatorio");
    UUID actor = Objects.requireNonNull(command.actorId(), "El actor es obligatorio");
    PeriodoPrestamo periodo = new PeriodoPrestamo(command.desde(), command.hasta());
    CatalogoPort.PublicacionSnapshot publicacion =
        catalogo.obtenerSnapshot(command.publicacionId(), periodo);
    if (!command.publicacionId().equals(publicacion.id())) {
      throw new DependencyUnavailableException("Catálogo devolvió una publicación distinta");
    }

    boolean snapshotCoincide = command.aceptacionExplicita()
        && secureEquals(command.snapshotId(), publicacion.snapshotId())
        && secureEquals(normalizeHash(command.snapshotHash()), normalizeHash(publicacion.snapshotHash()));
    SolicitudEligibilityPort.EligibilityFacts facts = eligibility.obtain(actor);
    ElegibilidadSolicitud candidate = new ElegibilidadSolicitud(
        command.correoFirebaseVerificado() ? EstadoComprobacion.CUMPLE : EstadoComprobacion.NO_CUMPLE,
        facts.verificacionInstitucional(), facts.terminosGenerales(),
        snapshotCoincide ? EstadoComprobacion.CUMPLE : EstadoComprobacion.NO_CUMPLE,
        publicacion.activa() ? EstadoComprobacion.CUMPLE : EstadoComprobacion.NO_CUMPLE,
        publicacion.periodoOfrecido() ? EstadoComprobacion.CUMPLE : EstadoComprobacion.NO_CUMPLE);
    validarElegibilidad(SolicitudSpecifications.completas().evaluate(candidate));

    if (actor.equals(publicacion.propietarioId())) {
      throw new BusinessConflictException("No puedes solicitar tu propio objeto");
    }
    if (persistence.existsCommittedOverlap(publicacion.id(), command.desde(), command.hasta())) {
      throw new BusinessConflictException("Periodo ya reservado");
    }
    if (persistence.existsPendingDuplicate(publicacion.id(), actor, command.desde(), command.hasta())) {
      throw new BusinessConflictException("Ya existe una solicitud pendiente para el mismo periodo");
    }

    Instant now = clock.instant();
    AceptacionCondiciones aceptacion = new AceptacionCondiciones(publicacion.snapshotId(),
        publicacion.snapshotHash(), now);
    Solicitud solicitud = Solicitud.crear(UUID.randomUUID(), publicacion.id(), actor,
        publicacion.propietarioId(), periodo, publicacion.condiciones(), aceptacion, now);
    solicitud = persistence.saveSolicitud(solicitud);
    append("SolicitudCreada", solicitud.id(), solicitud.aggregateVersion(),
        solicitudCreadaPayload(solicitud), correlation.currentId());
    audit.record("SOLICITUD", solicitud.id(), "CREAR", null, "PENDIENTE", actor,
        correlation.currentId());
    return SolicitudView.from(solicitud);
  }

  @Override
  @Transactional(readOnly = true)
  public List<SolicitudView> listarSolicitudes(UUID actor, boolean admin, String rol,
      String estado) {
    return persistence.findSolicitudes(actor, admin, rol, estado).stream()
        .map(SolicitudView::from).toList();
  }

  @Override
  @Transactional(readOnly = true)
  public SolicitudView obtenerSolicitud(UUID actor, boolean admin, UUID id) {
    Solicitud solicitud = solicitud(id);
    authorize(actor, admin, solicitud);
    return SolicitudView.from(solicitud);
  }

  @Override
  @Transactional
  public ReservaView aceptarSolicitud(UUID actor, UUID id) {
    Solicitud solicitud = persistence.lockSolicitud(id)
        .orElseThrow(() -> new NotFoundException("Solicitud no encontrada"));
    if (!actor.equals(solicitud.prestamistaId())) {
      throw new ForbiddenException("Solo el prestamista puede aceptar");
    }
    if (solicitud.estado() != EstadoSolicitud.PENDIENTE) {
      throw new BusinessConflictException("Solicitud no pendiente");
    }
    persistence.ensureAgenda(solicitud.publicacionId());
    AgendaDelObjeto agenda = persistence.lockAgenda(solicitud.publicacionId());
    Instant now = clock.instant();
    Reserva reserva;
    try {
      reserva = reservaFactory.crearConfirmada(UUID.randomUUID(), solicitud, agenda, now);
      solicitud.aceptar(actor, now);
    } catch (IllegalStateException exception) {
      throw new BusinessConflictException(exception.getMessage());
    } catch (SecurityException exception) {
      throw new ForbiddenException(exception.getMessage());
    }
    reserva = persistence.saveReserva(reserva, solicitud);
    persistence.saveSolicitud(solicitud);
    append("ReservaConfirmada", reserva.id(), reserva.aggregateVersion(),
        reservaConfirmadaPayload(reserva, solicitud), correlation.currentId());
    audit.record("SOLICITUD", solicitud.id(), "ACEPTAR", "PENDIENTE", "ACEPTADA", actor,
        correlation.currentId());
    audit.record("RESERVA", reserva.id(), "CONFIRMAR", null, "CONFIRMADA", actor,
        correlation.currentId());
    return ReservaView.from(reserva);
  }

  @Override
  @Transactional
  public SolicitudView rechazarSolicitud(UUID actor, UUID id, String motivo) {
    Solicitud solicitud = persistence.lockSolicitud(id)
        .orElseThrow(() -> new NotFoundException("Solicitud no encontrada"));
    String previous = solicitud.estado().name();
    try {
      solicitud.rechazar(actor, clock.instant(), motivo);
    } catch (SecurityException exception) {
      throw new ForbiddenException(exception.getMessage());
    } catch (IllegalStateException exception) {
      throw new BusinessConflictException(exception.getMessage());
    }
    solicitud = persistence.saveSolicitud(solicitud);
    append("SolicitudRechazada", solicitud.id(), solicitud.aggregateVersion(),
        Map.of("solicitud_id", solicitud.id(), "motivo_rechazo", solicitud.motivoRechazo()),
        correlation.currentId());
    audit.record("SOLICITUD", id, "RECHAZAR", previous, solicitud.estado().name(), actor,
        correlation.currentId());
    return SolicitudView.from(solicitud);
  }

  @Override
  @Transactional
  public SolicitudView cancelarSolicitud(UUID actor, UUID id, String motivo) {
    Solicitud solicitud = persistence.lockSolicitud(id)
        .orElseThrow(() -> new NotFoundException("Solicitud no encontrada"));
    String previous = solicitud.estado().name();
    try {
      solicitud.cancelar(actor, clock.instant(), motivo);
    } catch (SecurityException exception) {
      throw new ForbiddenException(exception.getMessage());
    } catch (IllegalStateException exception) {
      throw new BusinessConflictException(exception.getMessage());
    }
    solicitud = persistence.saveSolicitud(solicitud);
    append("SolicitudCancelada", solicitud.id(), solicitud.aggregateVersion(),
        Map.of("solicitud_id", solicitud.id(), "motivo_cancelacion",
            solicitud.motivoCancelacion(), "cancelada_por_usuario_id", actor),
        correlation.currentId());
    audit.record("SOLICITUD", id, "CANCELAR", previous, solicitud.estado().name(), actor,
        correlation.currentId());
    return SolicitudView.from(solicitud);
  }

  @Override
  @Transactional(readOnly = true)
  public List<ReservaView> listarReservas(UUID actor, boolean admin, String rol) {
    return persistence.findReservas(actor, admin, rol).stream().map(ReservaView::from).toList();
  }

  @Override
  @Transactional
  public ReservaView solicitarCancelacionReserva(UUID actor, boolean admin, UUID id,
      String motivo, String correlationId) {
    Reserva reserva = persistence.lockReserva(id)
        .orElseThrow(() -> new NotFoundException("Reserva no encontrada"));
    Solicitud solicitud = persistence.findSolicitudForReserva(id)
        .orElseThrow(() -> new NotFoundException("Solicitud de reserva no encontrada"));
    authorize(actor, admin, solicitud);
    try {
      reserva.solicitarCancelacion(actor, motivo, clock.instant());
    } catch (IllegalStateException exception) {
      throw new BusinessConflictException(exception.getMessage());
    }
    reserva = persistence.saveReserva(reserva, solicitud);
    append("CancelacionReservaSolicitada", id, reserva.aggregateVersion(),
        Map.of("reserva_id", id, "solicitud_id", solicitud.id(),
            "solicitada_por_usuario_id", actor, "motivo_cancelacion", motivo), correlationId);
    audit.record("RESERVA", id, "SOLICITAR_CANCELACION", "CONFIRMADA",
        "CANCELACION_PENDIENTE", actor, correlationId);
    return ReservaView.from(reserva);
  }

  @Override
  @Transactional(readOnly = true)
  public ContactoView obtenerContacto(UUID actor, boolean admin, UUID id) {
    Reserva reserva = persistence.findReserva(id)
        .orElseThrow(() -> new NotFoundException("Reserva no encontrada"));
    Solicitud solicitud = persistence.findSolicitudForReserva(id)
        .orElseThrow(() -> new NotFoundException("Solicitud de reserva no encontrada"));
    boolean prestatario = actor.equals(solicitud.prestatarioId());
    boolean prestamista = actor.equals(solicitud.prestamistaId());
    if (!prestatario && !prestamista) {
      throw new ForbiddenException("El contacto solo está disponible para participantes");
    }
    if (reserva.estado() != EstadoReserva.CONFIRMADA
        && reserva.estado() != EstadoReserva.CANCELACION_PENDIENTE) {
      throw new ForbiddenException("Contacto no disponible");
    }
    if (!prestamoAccess.contactoVigente(id, actor)) {
      throw new ForbiddenException("El préstamo ya no permite consultar el contacto");
    }
    UUID contraparte = prestatario ? solicitud.prestamistaId() : solicitud.prestatarioId();
    return new ContactoView(identidad.obtenerTelefonoAutorizado(contraparte, id));
  }

  private void validarElegibilidad(com.lendup.reservas.domain.specification.SpecificationResult result) {
    if (!result.pendingChecks().isEmpty()) {
      throw new DependencyUnavailableException("No fue posible comprobar la elegibilidad requerida");
    }
    Set<String> violations = result.violations();
    if (violations.contains("PUBLICACION_NO_ACTIVA")) {
      throw new BusinessConflictException("Objeto no disponible");
    }
    if (violations.contains("PERIODO_NO_DISPONIBLE")) {
      throw new BusinessConflictException("Periodo fuera de la disponibilidad publicada");
    }
    if (violations.contains("CONDICIONES_PUBLICACION_NO_ACEPTADAS")) {
      throw new BusinessConflictException("El snapshot aceptado está obsoleto o no coincide");
    }
    if (!violations.isEmpty()) {
      throw new ForbiddenException("El estudiante no cumple la elegibilidad requerida");
    }
  }

  private Solicitud solicitud(UUID id) {
    return persistence.findSolicitud(id)
        .orElseThrow(() -> new NotFoundException("Solicitud no encontrada"));
  }

  private void authorize(UUID actor, boolean admin, Solicitud solicitud) {
    if (!admin && !actor.equals(solicitud.prestatarioId())
        && !actor.equals(solicitud.prestamistaId())) {
      throw new ForbiddenException("No autorizado");
    }
  }

  private void append(String type, UUID aggregateId, long aggregateVersion,
      Map<String, Object> payload, String correlationId) {
    outbox.append(new EventEnvelope(UUID.randomUUID(), type, 1, aggregateVersion, aggregateId,
        clock.instant(), correlationId == null ? "" : correlationId, payload));
  }

  private Map<String, Object> solicitudCreadaPayload(Solicitud solicitud) {
    Map<String, Object> payload = new LinkedHashMap<>();
    payload.put("solicitud_id", solicitud.id());
    payload.put("publicacion_id", solicitud.publicacionId());
    payload.put("prestatario_usuario_id", solicitud.prestatarioId());
    payload.put("prestamista_usuario_id", solicitud.prestamistaId());
    payload.put("desde", solicitud.periodo().desde().toString());
    payload.put("hasta", solicitud.periodo().hasta().toString());
    payload.put("condiciones_snapshot_id", solicitud.aceptacionCondiciones().snapshotId());
    payload.put("condiciones_snapshot_hash", solicitud.aceptacionCondiciones().snapshotHash());
    return payload;
  }

  private Map<String, Object> reservaConfirmadaPayload(Reserva reserva, Solicitud solicitud) {
    CondicionesAcordadas c = reserva.condiciones();
    Map<String, Object> payload = new LinkedHashMap<>();
    payload.put("reserva_id", reserva.id());
    payload.put("solicitud_id", solicitud.id());
    payload.put("publicacion_id", solicitud.publicacionId());
    payload.put("prestamista_usuario_id", solicitud.prestamistaId());
    payload.put("prestatario_usuario_id", solicitud.prestatarioId());
    payload.put("desde", reserva.periodoContractual().desde().toString());
    payload.put("hasta", reserva.periodoContractual().hasta().toString());
    payload.put("tarifa_diaria_acordada", c.tarifaDiaria());
    payload.put("garantia_monetaria_acordada", c.garantiaMonetaria());
    payload.put("moneda_acordada", c.moneda());
    payload.put("lugar_intercambio_acordado", c.lugarIntercambio());
    payload.put("condiciones_uso_acordadas", c.uso());
    payload.put("condiciones_entrega_acordadas", c.entrega());
    payload.put("condiciones_devolucion_acordadas", c.devolucion());
    payload.put("condiciones_cancelacion_acordadas", c.cancelacion());
    payload.put("condiciones_snapshot_id", reserva.aceptacionCondiciones().snapshotId());
    payload.put("condiciones_snapshot_hash", reserva.aceptacionCondiciones().snapshotHash());
    payload.put("condiciones_aceptadas_en", reserva.aceptacionCondiciones().aceptadaEn());
    return payload;
  }

  private static boolean secureEquals(String left, String right) {
    if (left == null || right == null) return false;
    return MessageDigest.isEqual(left.getBytes(StandardCharsets.UTF_8),
        right.getBytes(StandardCharsets.UTF_8));
  }

  private static String normalizeHash(String value) {
    return value == null ? null : value.trim().toLowerCase(Locale.ROOT);
  }
}
