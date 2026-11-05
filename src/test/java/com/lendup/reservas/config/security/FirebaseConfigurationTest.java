package com.lendup.reservas.config.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseToken;
import java.util.Map;
import org.junit.jupiter.api.Test;

class FirebaseConfigurationTest {
  private final FirebaseConfiguration configuration = new FirebaseConfiguration();

  @Test
  void rechazaTokenDelProyectoFirebaseAnterior() throws Exception {
    FirebaseAuth auth = mock(FirebaseAuth.class);
    FirebaseToken token = mock(FirebaseToken.class);
    when(auth.verifyIdToken("old-project-token", true)).thenReturn(token);
    when(token.getClaims()).thenReturn(Map.of("aud", "lendup-3cb42"));

    TokenVerifier verifier = configuration.firebaseTokenVerifier(auth, "lendup-desarrollo");

    assertThrows(SecurityException.class, () -> verifier.verify("old-project-token"));
  }

  @Test
  void aceptaClaimsVerificadosDelProyectoCompartido() throws Exception {
    FirebaseAuth auth = mock(FirebaseAuth.class);
    FirebaseToken token = mock(FirebaseToken.class);
    when(auth.verifyIdToken("shared-project-token", true)).thenReturn(token);
    when(token.getClaims()).thenReturn(Map.of("aud", "lendup-desarrollo", "role", "ADMINISTRADOR"));
    when(token.getUid()).thenReturn("firebase-uid");
    when(token.isEmailVerified()).thenReturn(true);

    VerifiedToken verified = configuration
        .firebaseTokenVerifier(auth, "lendup-desarrollo")
        .verify("shared-project-token");

    assertEquals("firebase-uid", verified.uid());
    assertEquals(true, verified.emailVerified());
    assertEquals(true, verified.administrator());
  }
}
