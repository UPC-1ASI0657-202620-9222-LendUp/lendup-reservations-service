package com.lendup.reservas.domain.model;
import java.util.UUID;
public record AgendaDelObjeto(UUID id, UUID publicacionId, long version) {}
