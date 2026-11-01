package com.lendup.reservas.application.port.out;
import java.util.UUID;
public interface IdentidadPort { UUID resolverUsuarioId(String firebaseUid); String obtenerTelefonoAutorizado(UUID usuarioId, UUID reservaId); }
