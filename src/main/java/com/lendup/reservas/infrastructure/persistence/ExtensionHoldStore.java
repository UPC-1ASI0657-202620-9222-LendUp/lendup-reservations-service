package com.lendup.reservas.infrastructure.persistence;

import java.time.*;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class ExtensionHoldStore {
  public enum Status { RETENIDA, CONFIRMADA, RECHAZADA }

  public record Hold(UUID changeId, UUID publicationId, UUID reservationId, UUID loanId,
                     LocalDateTime startsAt, LocalDateTime endsAt, Instant expiresAt, Status status) {}

  private final JdbcClient db;

  public ExtensionHoldStore(JdbcClient db) { this.db = db; }

  public Optional<Hold> find(UUID id) {
    return db.sql("SELECT * FROM extension_holds WHERE change_id = ?").param(id.toString())
        .query(ExtensionHoldStore::row).optional();
  }

  public List<Hold> active(UUID publicationId, Instant now) {
    return db.sql("SELECT * FROM extension_holds WHERE publication_id = ? AND status = 'RETENIDA' AND expires_at > ?")
        .params(publicationId.toString(), utc(now)).query(ExtensionHoldStore::row).list();
  }

  public List<Hold> expired(Instant now) {
    return db.sql("SELECT * FROM extension_holds WHERE status = 'RETENIDA' AND expires_at <= ? ORDER BY expires_at LIMIT 50")
        .param(utc(now)).query(ExtensionHoldStore::row).list();
  }

  public void insert(Hold hold) {
    db.sql("""
        INSERT INTO extension_holds(change_id, publication_id, reservation_id, loan_id, starts_at, ends_at, expires_at, status)
        VALUES (?, ?, ?, ?, ?, ?, ?, ?)
        """).params(hold.changeId().toString(), hold.publicationId().toString(), hold.reservationId().toString(),
        hold.loanId().toString(), hold.startsAt(), hold.endsAt(), utc(hold.expiresAt()), hold.status().name()).update();
  }

  public void finish(UUID id, Status status) {
    db.sql("UPDATE extension_holds SET status = ? WHERE change_id = ? AND status = 'RETENIDA'")
        .params(status.name(), id.toString()).update();
  }

  private static Hold row(java.sql.ResultSet rs, int n) throws java.sql.SQLException {
    return new Hold(UUID.fromString(rs.getString("change_id")), UUID.fromString(rs.getString("publication_id")),
        UUID.fromString(rs.getString("reservation_id")), UUID.fromString(rs.getString("loan_id")),
        rs.getObject("starts_at", LocalDateTime.class), rs.getObject("ends_at", LocalDateTime.class),
        rs.getObject("expires_at", LocalDateTime.class).toInstant(ZoneOffset.UTC), Status.valueOf(rs.getString("status")));
  }

  private static LocalDateTime utc(Instant time) { return LocalDateTime.ofInstant(time, ZoneOffset.UTC); }
}
