package com.webar.app.dto;

import com.webar.app.entity.ThreeDGenerationJobStatus;
import java.time.LocalDateTime;

public record ThreeDGenerationJobResponse(
        Long id,
        Long menuItemId,
        String provider,
        String providerTaskId,
        String generationType,
        Integer progress,
        ThreeDGenerationJobStatus status,
        Integer sourceMediaCount,
        String errorMessage,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        LocalDateTime completedAt
) {}
