package com.lendup.reservas.support;
import com.lendup.reservas.config.security.*; import org.springframework.boot.test.context.TestConfiguration; import org.springframework.context.annotation.Bean;
@TestConfiguration public class TestAuthConfiguration { @Bean TokenVerifier testTokenVerifier(){return t->new VerifiedToken("11111111-1111-1111-1111-111111111111",true,false);} }
