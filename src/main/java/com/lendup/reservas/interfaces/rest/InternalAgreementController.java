package com.lendup.reservas.interfaces.rest;

import com.lendup.reservas.application.service.ReservationAgreementQuery;
import io.swagger.v3.oas.annotations.Hidden;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;
import org.springframework.web.bind.annotation.*;

@Hidden
@RestController
@RequestMapping("/internal/v1/reservas")
public class InternalAgreementController {
  private final ReservationAgreementQuery agreements;

  public InternalAgreementController(ReservationAgreementQuery agreements) {
    this.agreements = agreements;
  }

  record AgreementResponse(UUID id, LocalDateTime desde, LocalDateTime hasta, BigDecimal tarifa_diaria_acordada,
      BigDecimal garantia_monetaria_acordada, String moneda_acordada, String lugar_intercambio_acordado,
      String condiciones_uso_acordadas, String condiciones_entrega_acordadas, String condiciones_devolucion_acordadas,
      String condiciones_cancelacion_acordadas, String condiciones_snapshot_id, String condiciones_snapshot_hash,
      Instant condiciones_aceptadas_en) {}

  @GetMapping("/{id}/acuerdo")
  AgreementResponse agreement(@PathVariable UUID id) {
    var agreement = agreements.obtener(id);
    var c = agreement.condiciones();
    var a = agreement.aceptacion();
    return new AgreementResponse(agreement.id(), agreement.periodo().desde(), agreement.periodo().hasta(),
        c.tarifaDiaria(), c.garantiaMonetaria(), c.moneda(), c.lugarIntercambio(), c.uso(), c.entrega(), c.devolucion(), c.cancelacion(),
        a == null ? null : a.snapshotId(), a == null ? null : a.snapshotHash(), a == null ? null : a.aceptadaEn());
  }
}
