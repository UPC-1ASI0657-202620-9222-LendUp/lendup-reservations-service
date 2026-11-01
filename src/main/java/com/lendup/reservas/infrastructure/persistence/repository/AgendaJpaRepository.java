package com.lendup.reservas.infrastructure.persistence.repository;
import com.lendup.reservas.infrastructure.persistence.entity.AgendaEntity;
import jakarta.persistence.LockModeType;
import java.util.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
public interface AgendaJpaRepository extends JpaRepository<AgendaEntity,UUID>{
  @Modifying @Query(value="INSERT IGNORE INTO agendas_objeto(id,publicacion_id,version,creada_en,actualizada_en) VALUES(UUID_TO_BIN(:id),UUID_TO_BIN(:publicationId),0,UTC_TIMESTAMP(6),UTC_TIMESTAMP(6))",nativeQuery=true)
  int insertIfAbsent(@Param("id") String id,@Param("publicationId") String publicationId);
  @Lock(LockModeType.PESSIMISTIC_WRITE) @Query("select a from AgendaEntity a where a.publicacionId=:publicationId") Optional<AgendaEntity> lockByPublicationId(@Param("publicationId") UUID publicationId);
}
