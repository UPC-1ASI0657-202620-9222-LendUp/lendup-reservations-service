package com.lendup.reservas.infrastructure.persistence;

import com.lendup.reservas.infrastructure.messaging.OutboxBatchStore;
import com.lendup.reservas.infrastructure.persistence.entity.OutboxEventEntity;
import com.lendup.reservas.infrastructure.persistence.repository.OutboxJpaRepository;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class JpaOutboxBatchStore implements OutboxBatchStore {
  private final OutboxJpaRepository repository;
  public JpaOutboxBatchStore(OutboxJpaRepository repository) { this.repository = repository; }
  @Override public List<OutboxEventEntity> lockPending(int limit, int maxAttempts) { return repository.lockPending(limit, maxAttempts); }
  @Override public void save(OutboxEventEntity event) { repository.save(event); }
}
