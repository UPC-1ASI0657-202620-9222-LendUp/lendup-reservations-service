package com.lendup.reservas.config;
import com.lendup.reservas.domain.model.ReservaFactory; import java.time.Clock; import org.springframework.context.annotation.*;
@Configuration public class CoreConfiguration { @Bean Clock utcClock(){return Clock.systemUTC();} @Bean ReservaFactory reservaFactory(){return new ReservaFactory();} }
