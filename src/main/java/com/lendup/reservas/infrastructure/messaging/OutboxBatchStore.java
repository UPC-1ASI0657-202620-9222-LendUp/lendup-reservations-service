package com.lendup.reservas.infrastructure.messaging;

import com.lendup.reservas.infrastructure.persistence.entity.OutboxEventEntity;
import java.util.List;

public interface OutboxBatchStore {
  List<OutboxEventEntity> lockPending(int limit, int maxAttempts);
  void save(OutboxEventEntity event);
}
