package com.lendup.reservas.config.security;
public interface TokenVerifier { VerifiedToken verify(String idToken) throws Exception; }
