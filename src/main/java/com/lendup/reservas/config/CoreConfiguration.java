package com.lendup.reservas.config;
import java.time.Clock; import org.springframework.context.annotation.*;
@Configuration public class CoreConfiguration { @Bean Clock utcClock(){return Clock.systemUTC();} }
