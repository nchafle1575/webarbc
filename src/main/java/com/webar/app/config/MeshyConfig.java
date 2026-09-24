package com.webar.app.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "meshy")
public record MeshyConfig(
        String apiKey,
        String baseUrl,
        String aiModel,
        long pollIntervalMs
) {
    public MeshyConfig {
        if (baseUrl == null || baseUrl.isBlank()) {
            baseUrl = "https://api.meshy.ai";
        }
        if (aiModel == null || aiModel.isBlank()) {
            aiModel = "latest";
        }
        if (pollIntervalMs <= 0) {
            pollIntervalMs = 10000L;
        }
    }
}
