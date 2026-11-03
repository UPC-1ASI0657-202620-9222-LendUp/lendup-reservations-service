package com.lendup.reservas.integration;

import static org.junit.jupiter.api.Assertions.*;

import java.sql.*;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationVersion;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.*;
import org.testcontainers.mysql.MySQLContainer;

@Testcontainers(disabledWithoutDocker = true)
class HistoricalMigrationIT {
  @Container static final MySQLContainer mysql = new MySQLContainer("mysql:8.4")
      .withDatabaseName("lendup_reservas").withUsername("test").withPassword("test");

  @Test void migraDatosV1SinInventarConsentimientoNiAlterarPeriodoOriginal() throws Exception {
    Flyway v1 = Flyway.configure().dataSource(mysql.getJdbcUrl(), mysql.getUsername(),
        mysql.getPassword()).target(MigrationVersion.fromVersion("1")).load();
    v1.migrate();
    insertHistoricalData();

    Flyway latest = Flyway.configure().dataSource(mysql.getJdbcUrl(), mysql.getUsername(),
        mysql.getPassword()).load();
    assertEquals(2, latest.migrate().migrationsExecuted);

    try (Connection connection = DriverManager.getConnection(mysql.getJdbcUrl(),
        mysql.getUsername(), mysql.getPassword())) {
      try (Statement statement = connection.createStatement();
          ResultSet request = statement.executeQuery("""
              SELECT consentimiento_verificado, condiciones_snapshot_id,
                     condiciones_snapshot_hash, condiciones_aceptadas_en,
                     aggregate_version
              FROM solicitudes WHERE id=UUID_TO_BIN('11111111-1111-1111-1111-111111111111')
              """)) {
        assertTrue(request.next());
        assertFalse(request.getBoolean("consentimiento_verificado"));
        assertNull(request.getString("condiciones_snapshot_id"));
        assertNull(request.getString("condiciones_snapshot_hash"));
        assertNotNull(request.getTimestamp("condiciones_aceptadas_en"));
        assertEquals(1, request.getLong("aggregate_version"));
      }
      try (Statement statement = connection.createStatement();
          ResultSet reservation = statement.executeQuery("""
              SELECT desde,hasta,ocupacion_desde,ocupacion_hasta,periodo_liberado_desde
              FROM reservas WHERE id=UUID_TO_BIN('44444444-4444-4444-4444-444444444444')
              """)) {
        assertTrue(reservation.next());
        assertEquals(reservation.getTimestamp("desde"), reservation.getTimestamp("ocupacion_desde"));
        assertEquals(reservation.getTimestamp("hasta"), reservation.getTimestamp("ocupacion_hasta"));
        assertNull(reservation.getTimestamp("periodo_liberado_desde"));
      }
      assertTrue(indexExists(connection, "solicitudes", "uq_solicitudes_pendiente_dedup"));
      assertTrue(indexExists(connection, "reservas", "idx_reservas_agenda_ocupacion"));
      assertTrue(indexExists(connection, "outbox_events", "uq_outbox_aggregate_version"));
    }
  }

  private void insertHistoricalData() throws Exception {
    try (Connection connection = DriverManager.getConnection(mysql.getJdbcUrl(),
        mysql.getUsername(), mysql.getPassword()); Statement statement = connection.createStatement()) {
      statement.executeUpdate("""
          INSERT INTO solicitudes(id,publicacion_id,prestatario_usuario_id,
            prestamista_usuario_id,desde,hasta,estado,tarifa_diaria_aceptada,
            garantia_monetaria_aceptada,moneda_aceptada,lugar_intercambio_aceptado,
            condiciones_uso_aceptadas,condiciones_entrega_aceptadas,
            condiciones_devolucion_aceptadas,condiciones_cancelacion_aceptadas,
            condiciones_aceptadas_en,creada_en,actualizada_en)
          VALUES(UUID_TO_BIN('11111111-1111-1111-1111-111111111111'),
            UUID_TO_BIN('22222222-2222-2222-2222-222222222222'),
            UUID_TO_BIN('33333333-3333-3333-3333-333333333331'),
            UUID_TO_BIN('33333333-3333-3333-3333-333333333332'),
            '2026-11-10 09:00:00','2026-11-12 09:00:00','ACEPTADA',10.00,NULL,
            'PEN','Campus','uso','entrega','devolucion','cancelacion',
            '2026-11-01 12:00:00','2026-11-01 12:00:00','2026-11-01 12:00:00')
          """);
      statement.executeUpdate("""
          INSERT INTO agendas_objeto(id,publicacion_id,version,creada_en,actualizada_en)
          VALUES(UUID_TO_BIN('55555555-5555-5555-5555-555555555555'),
            UUID_TO_BIN('22222222-2222-2222-2222-222222222222'),0,
            '2026-11-01 12:00:00','2026-11-01 12:00:00')
          """);
      statement.executeUpdate("""
          INSERT INTO reservas(id,solicitud_id,agenda_id,desde,hasta,estado,
            tarifa_diaria_acordada,garantia_monetaria_acordada,moneda_acordada,
            lugar_intercambio_acordado,condiciones_uso_acordadas,
            condiciones_entrega_acordadas,condiciones_devolucion_acordadas,
            condiciones_cancelacion_acordadas,confirmada_en,external_event_version,
            actualizada_en)
          VALUES(UUID_TO_BIN('44444444-4444-4444-4444-444444444444'),
            UUID_TO_BIN('11111111-1111-1111-1111-111111111111'),
            UUID_TO_BIN('55555555-5555-5555-5555-555555555555'),
            '2026-11-10 09:00:00','2026-11-12 09:00:00','CONFIRMADA',10.00,NULL,
            'PEN','Campus','uso','entrega','devolucion','cancelacion',
            '2026-11-01 12:00:00',0,'2026-11-01 12:00:00')
          """);
    }
  }

  private boolean indexExists(Connection connection, String table, String index) throws Exception {
    try (PreparedStatement statement = connection.prepareStatement(
        "SELECT COUNT(*) FROM information_schema.statistics "
            + "WHERE table_schema=DATABASE() AND table_name=? AND index_name=?")) {
      statement.setString(1, table);
      statement.setString(2, index);
      try (ResultSet result = statement.executeQuery()) {
        result.next();
        return result.getInt(1) > 0;
      }
    }
  }
}
