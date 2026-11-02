package com.lendup.reservas.domain.specification;

import java.util.Objects;

public final class SolicitudSpecifications {
  private SolicitudSpecifications() {}

  public static Specification<ElegibilidadSolicitud> correoFirebaseVerificado() {
    return candidate -> comprobar(candidate.correoFirebase(), "CORREO_FIREBASE_NO_VERIFICADO", "CORREO_FIREBASE_DESCONOCIDO");
  }

  public static Specification<ElegibilidadSolicitud> estudianteVerificado() {
    return candidate -> comprobar(candidate.verificacionInstitucional(), "ESTUDIANTE_NO_VERIFICADO", "VERIFICACION_INSTITUCIONAL_PENDIENTE");
  }

  public static Specification<ElegibilidadSolicitud> terminosAceptados() {
    return candidate -> comprobar(candidate.terminosGenerales(), "TERMINOS_GENERALES_NO_ACEPTADOS", "TERMINOS_GENERALES_PENDIENTES")
        .combine(comprobar(candidate.condicionesPublicacion(), "CONDICIONES_PUBLICACION_NO_ACEPTADAS", "CONDICIONES_PUBLICACION_PENDIENTES"));
  }

  public static Specification<ElegibilidadSolicitud> publicacionActiva() {
    return candidate -> comprobar(candidate.publicacionActiva(), "PUBLICACION_NO_ACTIVA", "ESTADO_PUBLICACION_DESCONOCIDO");
  }

  public static Specification<ElegibilidadSolicitud> periodoDisponible() {
    return candidate -> comprobar(candidate.periodoDisponible(), "PERIODO_NO_DISPONIBLE", "DISPONIBILIDAD_PENDIENTE");
  }

  public static Specification<ElegibilidadSolicitud> completas() {
    return correoFirebaseVerificado()
        .and(estudianteVerificado())
        .and(terminosAceptados())
        .and(publicacionActiva())
        .and(periodoDisponible());
  }

  private static SpecificationResult comprobar(EstadoComprobacion state, String violation, String pending) {
    return switch (state) {
      case CUMPLE -> SpecificationResult.passed();
      case NO_CUMPLE -> SpecificationResult.violated(violation);
      case DESCONOCIDO -> SpecificationResult.pending(pending);
    };
  }

  public enum EstadoComprobacion { CUMPLE, NO_CUMPLE, DESCONOCIDO }

  public record ElegibilidadSolicitud(
      EstadoComprobacion correoFirebase,
      EstadoComprobacion verificacionInstitucional,
      EstadoComprobacion terminosGenerales,
      EstadoComprobacion condicionesPublicacion,
      EstadoComprobacion publicacionActiva,
      EstadoComprobacion periodoDisponible) {
    public ElegibilidadSolicitud {
      Objects.requireNonNull(correoFirebase);
      Objects.requireNonNull(verificacionInstitucional);
      Objects.requireNonNull(terminosGenerales);
      Objects.requireNonNull(condicionesPublicacion);
      Objects.requireNonNull(publicacionActiva);
      Objects.requireNonNull(periodoDisponible);
    }
  }
}
