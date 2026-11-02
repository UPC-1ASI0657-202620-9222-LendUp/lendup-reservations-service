package com.lendup.reservas.infrastructure.persistence.repository;
import com.lendup.reservas.infrastructure.persistence.entity.ProcessedEventEntity; import java.time.Instant; import java.util.UUID; import org.springframework.data.jpa.repository.*; import org.springframework.data.repository.query.Param;
public interface ProcessedEventJpaRepository extends JpaRepository<ProcessedEventEntity,UUID>{
  @Modifying
  @Query(value="INSERT IGNORE INTO processed_events(event_id,consumer,aggregate_id,aggregate_version,processed_at) VALUES(UUID_TO_BIN(:eventId),:consumer,UUID_TO_BIN(:aggregateId),:aggregateVersion,:processedAt)",nativeQuery=true)
  int tryRegister(@Param("eventId") String eventId,@Param("consumer") String consumer,@Param("aggregateId") String aggregateId,@Param("aggregateVersion") long aggregateVersion,@Param("processedAt") Instant processedAt);
}
