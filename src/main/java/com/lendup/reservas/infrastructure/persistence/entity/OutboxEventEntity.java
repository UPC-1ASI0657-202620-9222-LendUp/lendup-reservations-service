package com.lendup.reservas.infrastructure.persistence.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "outbox_events")
public class OutboxEventEntity {
  @Id private UUID id;
  @Column(name = "aggregate_type", nullable = false) private String aggregateType;
  @Column(name = "aggregate_id", nullable = false) private UUID aggregateId;
  @Column(name = "event_type", nullable = false) private String eventType;
  @Column(name = "event_version", nullable = false) private int eventVersion;
  @Column(name = "aggregate_version", nullable = false) private long aggregateVersion;
  @Column(name = "payload", nullable = false, columnDefinition = "JSON") private String payload;
  @Column(name = "correlation_id", nullable = false) private String correlationId;
  @Column(name = "occurred_at", nullable = false) private Instant occurredAt;
  @Column(name = "published_at") private Instant publishedAt;
  @Column(name = "attempts", nullable = false) private int attempts;
  @Column(name = "last_error") private String lastError;
  @Column(name = "claimed_by") private String claimedBy;
  @Column(name = "claimed_at") private Instant claimedAt;
  @Column(name = "next_attempt_at") private Instant nextAttemptAt;

  protected OutboxEventEntity() {}

  public OutboxEventEntity(UUID id, String aggregateType, UUID aggregateId, String eventType,
      int eventVersion, long aggregateVersion, String payload, String correlationId,
      Instant occurredAt) {
    this.id = id;
    this.aggregateType = aggregateType;
    this.aggregateId = aggregateId;
    this.eventType = eventType;
    this.eventVersion = eventVersion;
    this.aggregateVersion = aggregateVersion;
    this.payload = payload;
    this.correlationId = correlationId;
    this.occurredAt = occurredAt;
  }

  public void claim(String owner, Instant now) {
    claimedBy = owner;
    claimedAt = now;
  }

  public void completeClaim(String owner, Instant now) {
    if (!owner.equals(claimedBy)) return;
    publishedAt = now;
    lastError = null;
    claimedBy = null;
    claimedAt = null;
    nextAttemptAt = null;
  }

  public void failClaim(String owner, String safeError, Instant nextAttempt) {
    if (!owner.equals(claimedBy)) return;
    attempts++;
    lastError = safeError;
    claimedBy = null;
    claimedAt = null;
    nextAttemptAt = nextAttempt;
  }

  public UUID getId() { return id; }
  public String getAggregateType() { return aggregateType; }
  public UUID getAggregateId() { return aggregateId; }
  public String getEventType() { return eventType; }
  public int getEventVersion() { return eventVersion; }
  public long getAggregateVersion() { return aggregateVersion; }
  public String getPayload() { return payload; }
  public String getCorrelationId() { return correlationId; }
  public Instant getOccurredAt() { return occurredAt; }
  public Instant getPublishedAt() { return publishedAt; }
  public int getAttempts() { return attempts; }
  public String getLastError() { return lastError; }
  public String getClaimedBy() { return claimedBy; }
}
