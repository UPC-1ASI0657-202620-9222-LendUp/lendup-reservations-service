package com.lendup.reservas.infrastructure.persistence.repository;

import com.lendup.reservas.infrastructure.persistence.entity.OutboxEventEntity;
import java.time.Instant;
import java.util.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface OutboxJpaRepository extends JpaRepository<OutboxEventEntity, UUID> {
  @Query(value = """
      SELECT o.* FROM outbox_events o
      WHERE o.published_at IS NULL
        AND o.attempts < :maxAttempts
        AND (o.next_attempt_at IS NULL OR o.next_attempt_at <= :now)
        AND (o.claimed_at IS NULL OR o.claimed_at < :staleBefore)
        AND NOT EXISTS (
          SELECT 1 FROM outbox_events earlier
          WHERE earlier.aggregate_type = o.aggregate_type
            AND earlier.aggregate_id = o.aggregate_id
            AND earlier.aggregate_version < o.aggregate_version
            AND earlier.published_at IS NULL
        )
      ORDER BY o.occurred_at, o.id
      LIMIT :batchSize
      FOR UPDATE SKIP LOCKED
      """, nativeQuery = true)
  List<OutboxEventEntity> lockClaimable(@Param("batchSize") int batchSize,
      @Param("maxAttempts") int maxAttempts, @Param("now") Instant now,
      @Param("staleBefore") Instant staleBefore);

  @Modifying
  @Query("update OutboxEventEntity o set o.publishedAt=:publishedAt,o.lastError=null,"
      + "o.claimedBy=null,o.claimedAt=null,o.nextAttemptAt=null "
      + "where o.id=:id and o.claimedBy=:owner and o.publishedAt is null")
  int markPublished(@Param("id") UUID id, @Param("owner") String owner,
      @Param("publishedAt") Instant publishedAt);

  @Modifying
  @Query("update OutboxEventEntity o set o.attempts=o.attempts+1,o.lastError=:error,"
      + "o.claimedBy=null,o.claimedAt=null,o.nextAttemptAt=:nextAttempt "
      + "where o.id=:id and o.claimedBy=:owner and o.publishedAt is null")
  int markFailed(@Param("id") UUID id, @Param("owner") String owner,
      @Param("error") String error, @Param("nextAttempt") Instant nextAttempt);

  long countByPublishedAtIsNullAndAttemptsGreaterThanEqual(int attempts);
}
