package com.lendup.reservas.contracts;

import static org.junit.jupiter.api.Assertions.*;

import com.lendup.reservas.interfaces.rest.dto.RequestDtos.*;
import com.lendup.reservas.interfaces.rest.dto.ResponseDtos.*;
import jakarta.validation.Validation;
import java.math.BigDecimal;
import java.time.*;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.json.JsonMapper;

class FrontendContractTest {
  private final JsonMapper json = JsonMapper.builder().findAndAddModules().build();

  @Test void crearSolicitudConservaCamposPreviosYExigeEvidenciaDeSnapshot() throws Exception {
    UUID id = UUID.randomUUID();
    String body = "{\"publicacion_id\":\"" + id
        + "\",\"desde\":\"2026-11-01T09:00:00\",\"hasta\":\"2026-11-02T09:00:00\","
        + "\"aceptacion_condiciones\":{\"snapshot_id\":\"v1\",\"snapshot_hash\":\""
        + "a".repeat(64) + "\",\"aceptada\":true}}";
    CrearSolicitud dto = json.readValue(body, CrearSolicitud.class);
    assertAll(
        () -> assertEquals(id, dto.publicacion_id()),
        () -> assertEquals(LocalDateTime.of(2026, 11, 1, 9, 0), dto.desde()),
        () -> assertTrue(dto.aceptacion_condiciones().aceptada()));
    try (var factory = Validation.buildDefaultValidatorFactory()) {
      assertTrue(factory.getValidator().validate(dto).isEmpty());
      CrearSolicitud legacy = json.readValue("{\"publicacion_id\":\"" + id
          + "\",\"desde\":\"2026-11-01T09:00:00\",\"hasta\":\"2026-11-02T09:00:00\"}",
          CrearSolicitud.class);
      assertFalse(factory.getValidator().validate(legacy).isEmpty());
    }
  }

  @Test void respuestasConservanCamposConsumidosPorElMapper() {
    var requestFields = java.util.Arrays.stream(SolicitudResponse.class.getRecordComponents())
        .map(java.lang.reflect.RecordComponent::getName).toList();
    var reservationFields = java.util.Arrays.stream(ReservaResponse.class.getRecordComponents())
        .map(java.lang.reflect.RecordComponent::getName).toList();
    assertTrue(requestFields.containsAll(java.util.List.of("publicacion_id", "desde", "hasta",
        "tarifa_diaria_aceptada", "condiciones_aceptadas_en")));
    assertTrue(reservationFields.containsAll(java.util.List.of("solicitud_id", "agenda_id",
        "tarifa_diaria_acordada", "garantia_monetaria_acordada", "moneda_acordada",
        "lugar_intercambio_acordado", "condiciones_uso_acordadas", "confirmada_en")));
  }

  @Test void jsonEstrictoRechazaIdentidadORolesInyectadosPorCliente() {
    var strict = JsonMapper.builder().findAndAddModules()
        .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES).build();
    String body = "{\"publicacion_id\":\"" + UUID.randomUUID()
        + "\",\"desde\":\"2026-11-01T09:00:00\",\"hasta\":\"2026-11-02T09:00:00\","
        + "\"prestatario_usuario_id\":\"" + UUID.randomUUID() + "\",\"rol\":\"ADMINISTRADOR\"}";
    assertThrows(Exception.class, () -> strict.readValue(body, CrearSolicitud.class));
  }

  @Test void motivosYConsentimientoSonObligatorios() {
    try (var factory = Validation.buildDefaultValidatorFactory()) {
      var validator = factory.getValidator();
      assertFalse(validator.validate(new RechazarSolicitud(" ")).isEmpty());
      assertFalse(validator.validate(new CancelarSolicitud(null)).isEmpty());
      assertFalse(validator.validate(new AceptacionCondicionesRequest("v1", "a".repeat(64), false)).isEmpty());
    }
  }

  @Test void conservaCampoDeConsentimientoCuandoNoExisteEvidenciaHistorica() throws Exception {
    Instant now = Instant.now();
    SolicitudResponse response = new SolicitudResponse(UUID.randomUUID(), UUID.randomUUID(),
        UUID.randomUUID(), UUID.randomUUID(), LocalDateTime.now(), LocalDateTime.now().plusDays(1),
        "PENDIENTE", BigDecimal.ONE, null, "PEN", "Campus", "u", "e", "d", "c",
        null, null, null, now, null, null, null, null, now);
    assertTrue(json.writeValueAsString(response).contains("\"condiciones_aceptadas_en\":null"));
  }
}
