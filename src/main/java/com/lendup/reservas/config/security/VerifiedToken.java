package com.lendup.reservas.config.security;
public record VerifiedToken(String uid,boolean emailVerified,boolean administrator){}
