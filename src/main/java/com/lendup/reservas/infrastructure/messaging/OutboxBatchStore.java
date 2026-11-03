package com.lendup.reservas.infrastructure.messaging;

import com.lendup.reservas.infrastructure.persistence.entity.OutboxEventEntity;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface OutboxBatchStore {
  List<OutboxEventEntity> claimPending(String owner, Instant now, Instant staleBefore,
      int limit, int maxAttempts);
  boolean markPublished(UUID eventId, String owner, Instant now);
  boolean markFailed(UUID eventId, String owner, String safeError, Instant nextAttempt);
}
