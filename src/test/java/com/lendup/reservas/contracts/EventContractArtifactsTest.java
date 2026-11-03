package com.lendup.reservas.contracts;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

class EventContractArtifactsTest {
  private final JsonMapper json = JsonMapper.builder().build();

  @Test void contratosYEjemplosSonJsonValidoYContienenLosEventosPublicos() throws Exception {
    JsonNode emitted = read("contracts/events/v1/reservations-events.schema.json");
    JsonNode consumed = read("contracts/events/v1/loan-events.schema.json");
    JsonNode examples = read("contracts/events/v1/examples.json");

    String emittedText = emitted.toString();
    assertAll(
        () -> assertTrue(emittedText.contains("SolicitudCreada")),
        () -> assertTrue(emittedText.contains("SolicitudRechazada")),
        () -> assertTrue(emittedText.contains("SolicitudCancelada")),
        () -> assertTrue(emittedText.contains("ReservaConfirmada")),
        () -> assertTrue(emittedText.contains("CancelacionReservaSolicitada")),
        () -> assertTrue(emittedText.contains("ReservaCancelada")),
        () -> assertTrue(emittedText.contains("condiciones_snapshot_hash")),
        () -> assertTrue(consumed.toString().contains("devolucion_confirmada_por_prestamista")),
        () -> assertTrue(examples.has("reserva_confirmada")),
        () -> assertTrue(examples.has("periodo_prestamo_liberado")));
  }

  private JsonNode read(String path) throws Exception {
    return json.readTree(Files.readString(Path.of(path)));
  }
}
