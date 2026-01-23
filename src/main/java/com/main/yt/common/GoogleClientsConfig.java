package com.main.yt.common;

import com.google.api.services.drive.Drive;
import com.google.api.services.youtube.YouTube;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GoogleClientsConfig {

    private final GoogleClientFactory factory;

    public GoogleClientsConfig(GoogleClientFactory factory) {
        this.factory = factory;
    }

    @Bean
    public Drive drive() throws Exception {
        return factory.drive();
    }

    @Bean
    public YouTube youtube() throws Exception {
        return factory.youtube();
    }
}

