package com.webar.app.dto.meshy;
import com.fasterxml.jackson.annotation.JsonProperty;

public record MeshyTaskResponse(
        String id,
        String type,
        @JsonProperty("model_urls") MeshyModelUrls modelUrls,
        @JsonProperty("thumbnail_url") String thumbnailUrl,
        Integer progress,
        String status,
        @JsonProperty("task_error") MeshyTaskError taskError
) {}
