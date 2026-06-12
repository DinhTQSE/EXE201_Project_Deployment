package com.vsign.backend.admin.persistence;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AdminReviewQueueRepository extends JpaRepository<AdminReviewQueueEntity, String> {
    List<AdminReviewQueueEntity> findAllByOrderByContentIdAsc();

    int countByStatus(String status);
}
