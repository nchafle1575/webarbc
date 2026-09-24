package com.webar.app.service;

import com.webar.app.config.MeshyConfig;
import com.webar.app.dto.meshy.MeshyCreateTaskResponse;
import com.webar.app.dto.meshy.MeshyTaskResponse;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

@Service
public class MeshyClient {

    private final RestClient restClient;
    private final MeshyConfig config;

    public MeshyClient(RestClient meshyRestClient, MeshyConfig config) {
        this.restClient = meshyRestClient;
        this.config = config;
    }

    public String createImageTo3DTask(String imageDataUri) {
        requireApiKey();

        MeshyCreateTaskResponse response = restClient.post()
                .uri("/openapi/v1/image-to-3d")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of(
                        "image_url", imageDataUri,
                        "ai_model", config.aiModel(),
                        "should_texture", true,
                        "enable_pbr", true,
                        "target_formats", List.of("glb", "usdz"),
                        "auto_size", true
                ))
                .retrieve()
                .body(MeshyCreateTaskResponse.class);

        if (response == null || response.result() == null || response.result().isBlank()) {
            throw new IllegalStateException("Meshy did not return a task id");
        }
        return response.result();
    }

    public String createMultiImageTo3DTask(List<String> imageDataUris) {
        requireApiKey();

        if (imageDataUris == null || imageDataUris.isEmpty() || imageDataUris.size() > 4) {
            throw new IllegalArgumentException("Meshy requires between 1 and 4 images");
        }

        MeshyCreateTaskResponse response = restClient.post()
                .uri("/openapi/v1/multi-image-to-3d")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of(
                        "image_urls", imageDataUris,
                        "ai_model", config.aiModel(),
                        "should_texture", true,
                        "enable_pbr", true,
                        "target_formats", List.of("glb", "usdz"),
                        "auto_size", true
                ))
                .retrieve()
                .body(MeshyCreateTaskResponse.class);

        if (response == null || response.result() == null || response.result().isBlank()) {
            throw new IllegalStateException("Meshy did not return a task id");
        }
        return response.result();
    }

    public MeshyTaskResponse getImageTask(String taskId) {
        requireApiKey();
        return restClient.get()
                .uri("/openapi/v1/image-to-3d/{id}", taskId)
                .retrieve()
                .body(MeshyTaskResponse.class);
    }

    public MeshyTaskResponse getMultiImageTask(String taskId) {
        requireApiKey();
        return restClient.get()
                .uri("/openapi/v1/multi-image-to-3d/{id}", taskId)
                .retrieve()
                .body(MeshyTaskResponse.class);
    }

    private void requireApiKey() {
        if (config.apiKey() == null || config.apiKey().isBlank()) {
            throw new IllegalStateException("MESHY_API_KEY is not configured");
        }
    }
}
