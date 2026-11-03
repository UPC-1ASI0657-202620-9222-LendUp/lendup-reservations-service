package com.lendup.reservas.domain.specification;

import static com.lendup.reservas.domain.specification.SolicitudSpecifications.EstadoComprobacion.*;
import static org.junit.jupiter.api.Assertions.*;

import com.lendup.reservas.domain.specification.SolicitudSpecifications.ElegibilidadSolicitud;
import org.junit.jupiter.api.Test;

class SolicitudSpecificationsTest {
  @Test void distingueCorreoFirebaseDeVerificacionInstitucional() {
    var candidate = new ElegibilidadSolicitud(CUMPLE, NO_CUMPLE, CUMPLE, CUMPLE, CUMPLE, CUMPLE);
    assertTrue(SolicitudSpecifications.correoFirebaseVerificado().evaluate(candidate).satisfied());
    var institutional = SolicitudSpecifications.estudianteVerificado().evaluate(candidate);
    assertFalse(institutional.satisfied());
    assertTrue(institutional.violations().contains("ESTUDIANTE_NO_VERIFICADO"));
  }

  @Test void terminosGeneralesYCondicionesDePublicacionSonComprobacionesSeparadas() {
    var candidate = new ElegibilidadSolicitud(CUMPLE, CUMPLE, CUMPLE, NO_CUMPLE, CUMPLE, CUMPLE);
    var result = SolicitudSpecifications.terminosAceptados().evaluate(candidate);
    assertFalse(result.satisfied());
    assertEquals(java.util.Set.of("CONDICIONES_PUBLICACION_NO_ACEPTADAS"), result.violations());
  }

  @Test void informacionDesconocidaNoSeTrataComoVerificada() {
    var candidate = new ElegibilidadSolicitud(CUMPLE, DESCONOCIDO, DESCONOCIDO, DESCONOCIDO, CUMPLE, CUMPLE);
    var result = SolicitudSpecifications.completas().evaluate(candidate);
    assertFalse(result.satisfied());
    assertEquals(3, result.pendingChecks().size());
  }

  @Test void especificacionesSeComponenSinPerderTodosLosIncumplimientos() {
    var candidate = new ElegibilidadSolicitud(NO_CUMPLE, NO_CUMPLE, CUMPLE, CUMPLE, NO_CUMPLE, NO_CUMPLE);
    var result = SolicitudSpecifications.completas().evaluate(candidate);
    assertFalse(result.satisfied());
    assertEquals(4, result.violations().size());
  }
}
