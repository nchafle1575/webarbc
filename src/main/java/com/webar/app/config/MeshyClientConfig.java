package com.webar.app.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.client.RestClient;

@Configuration
@EnableScheduling
@EnableConfigurationProperties(MeshyConfig.class)
public class MeshyClientConfig {
    @Bean
    public RestClient meshyRestClient(RestClient.Builder builder, MeshyConfig config) {
        return builder
                .baseUrl(config.baseUrl())
                .defaultHeader("Authorization", "Bearer " + config.apiKey())
                .build();
    }
}
