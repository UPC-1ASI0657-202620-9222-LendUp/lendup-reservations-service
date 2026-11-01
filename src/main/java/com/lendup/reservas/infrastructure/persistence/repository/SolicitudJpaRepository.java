package com.lendup.reservas.infrastructure.persistence.repository;
import com.lendup.reservas.infrastructure.persistence.entity.SolicitudEntity;
import jakarta.persistence.LockModeType;
import java.util.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
public interface SolicitudJpaRepository extends JpaRepository<SolicitudEntity,UUID>{
  @Lock(LockModeType.PESSIMISTIC_WRITE) @Query("select s from SolicitudEntity s where s.id=:id") Optional<SolicitudEntity> lockById(@Param("id") UUID id);
  @Query("select s from SolicitudEntity s where (:admin=true or (:rol='PRESTAMISTA' and s.prestamistaUsuarioId=:actor) or (:rol='PRESTATARIO' and s.prestatarioUsuarioId=:actor) or (:rol='' and (s.prestamistaUsuarioId=:actor or s.prestatarioUsuarioId=:actor))) and (:estado='' or cast(s.estado as string)=:estado) order by s.creadaEn desc")
  List<SolicitudEntity> search(@Param("actor") UUID actor,@Param("admin") boolean admin,@Param("rol") String rol,@Param("estado") String estado);
}
