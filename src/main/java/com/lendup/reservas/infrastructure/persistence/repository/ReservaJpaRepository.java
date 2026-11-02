package com.lendup.reservas.infrastructure.persistence.repository;
import com.lendup.reservas.infrastructure.persistence.entity.ReservaEntity;
import jakarta.persistence.LockModeType;
import java.time.LocalDateTime;
import java.util.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
public interface ReservaJpaRepository extends JpaRepository<ReservaEntity,UUID>{
  @Lock(LockModeType.PESSIMISTIC_WRITE) @Query("select r from ReservaEntity r join fetch r.solicitud where r.id=:id") Optional<ReservaEntity> lockById(@Param("id") UUID id);
  @Query("select r from ReservaEntity r where r.agendaId=:agendaId and r.estado in (com.lendup.reservas.domain.model.EstadoReserva.CONFIRMADA,com.lendup.reservas.domain.model.EstadoReserva.CANCELACION_PENDIENTE)")
  List<ReservaEntity> findBlockingByAgendaId(@Param("agendaId") UUID agendaId);
  @Query("select (count(r)>0) from ReservaEntity r where r.solicitud.publicacionId=:publicacionId and r.estado in (com.lendup.reservas.domain.model.EstadoReserva.CONFIRMADA,com.lendup.reservas.domain.model.EstadoReserva.CANCELACION_PENDIENTE) and r.desde<:hasta and r.hasta>:desde")
  boolean existsCommittedOverlap(@Param("publicacionId") UUID publicacionId,@Param("desde") LocalDateTime desde,@Param("hasta") LocalDateTime hasta);
  @Query("select r from ReservaEntity r join fetch r.solicitud s where (:admin=true or (:rol='PRESTAMISTA' and s.prestamistaUsuarioId=:actor) or (:rol='PRESTATARIO' and s.prestatarioUsuarioId=:actor) or (:rol='' and (s.prestamistaUsuarioId=:actor or s.prestatarioUsuarioId=:actor))) order by r.confirmadaEn desc")
  List<ReservaEntity> search(@Param("actor") UUID actor,@Param("admin") boolean admin,@Param("rol") String rol);
}
