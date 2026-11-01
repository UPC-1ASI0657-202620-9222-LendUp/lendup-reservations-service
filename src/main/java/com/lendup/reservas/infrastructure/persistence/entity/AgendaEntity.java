package com.lendup.reservas.infrastructure.persistence.entity;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
@Entity @Table(name="agendas_objeto")
public class AgendaEntity {
  @Id private UUID id;
  @Column(name="publicacion_id",nullable=false,unique=true) private UUID publicacionId;
  @Version private long version;
  @Column(name="creada_en",nullable=false) private Instant creadaEn;
  @Column(name="actualizada_en",nullable=false) private Instant actualizadaEn;
  protected AgendaEntity(){}
  public UUID getId(){return id;} public UUID getPublicacionId(){return publicacionId;} public long getVersion(){return version;}
}
