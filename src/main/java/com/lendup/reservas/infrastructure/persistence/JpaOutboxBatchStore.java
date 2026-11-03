package com.lendup.reservas.infrastructure.persistence;

import com.lendup.reservas.infrastructure.messaging.OutboxBatchStore;
import com.lendup.reservas.infrastructure.persistence.entity.OutboxEventEntity;
import com.lendup.reservas.infrastructure.persistence.repository.OutboxJpaRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
public class JpaOutboxBatchStore implements OutboxBatchStore {
  private final OutboxJpaRepository repository;
  public JpaOutboxBatchStore(OutboxJpaRepository repository) { this.repository = repository; }

  @Override
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public List<OutboxEventEntity> claimPending(String owner, Instant now, Instant staleBefore,
      int limit, int maxAttempts) {
    List<OutboxEventEntity> events = repository.lockClaimable(limit, maxAttempts, now, staleBefore);
    events.forEach(event -> event.claim(owner, now));
    return repository.saveAll(events);
  }

  @Override
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public boolean markPublished(UUID eventId, String owner, Instant now) {
    return repository.markPublished(eventId, owner, now) == 1;
  }

  @Override
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public boolean markFailed(UUID eventId, String owner, String safeError, Instant nextAttempt) {
    return repository.markFailed(eventId, owner, safeError, nextAttempt) == 1;
  }
}
