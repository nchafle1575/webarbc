package com.webar.app.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "meshy")
public record MeshyConfig(String apiKey, String baseUrl) {
    public MeshyConfig {
        if (baseUrl == null || baseUrl.isBlank()) baseUrl = "https://api.meshy.ai";
    }
}
