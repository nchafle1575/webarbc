package com.webar.app.repository;

import com.webar.app.entity.ThreeDGenerationJob;
import com.webar.app.entity.ThreeDGenerationJobStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface ThreeDGenerationJobRepository extends JpaRepository<ThreeDGenerationJob, Long> {
    Optional<ThreeDGenerationJob> findTopByMenuItemIdOrderByCreatedAtDesc(Long menuItemId);
    List<ThreeDGenerationJob> findByStatusIn(List<ThreeDGenerationJobStatus> statuses);
}
