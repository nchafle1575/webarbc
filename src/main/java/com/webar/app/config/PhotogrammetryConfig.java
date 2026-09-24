package com.webar.app.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "photogrammetry")
public record PhotogrammetryConfig(String workerUrl, long timeoutMs) {
    public PhotogrammetryConfig {
        if (workerUrl == null || workerUrl.isBlank()) workerUrl = "http://localhost:8000";
        if (timeoutMs <= 0) timeoutMs = 30000L;
    }
}
