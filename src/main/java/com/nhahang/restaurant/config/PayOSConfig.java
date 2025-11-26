package com.nhahang.restaurant.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PayOSConfig {
    @Bean
    public Object payOS() {
        return null; // PayOS integration disabled - return null placeholder
    }
}