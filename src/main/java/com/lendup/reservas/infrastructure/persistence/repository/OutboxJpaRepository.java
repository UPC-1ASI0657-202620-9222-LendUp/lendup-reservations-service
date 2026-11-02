package com.lendup.reservas.infrastructure.persistence.repository;
import com.lendup.reservas.infrastructure.persistence.entity.OutboxEventEntity;
import java.util.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
public interface OutboxJpaRepository extends JpaRepository<OutboxEventEntity,UUID>{
  @Query(value="SELECT * FROM outbox_events WHERE published_at IS NULL AND attempts < :maxAttempts ORDER BY occurred_at LIMIT :batchSize FOR UPDATE SKIP LOCKED",nativeQuery=true)
  List<OutboxEventEntity> lockPending(@Param("batchSize") int batchSize,@Param("maxAttempts") int maxAttempts);
  long countByPublishedAtIsNullAndAttemptsGreaterThanEqual(int attempts);
}
