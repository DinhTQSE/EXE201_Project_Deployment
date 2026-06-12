package com.vsign.backend.learning.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Collection;

public interface SignatureAttemptLogRepository extends JpaRepository<SignatureAttemptLogEntity, String> {
    boolean existsByUserKeyAndPracticeItemIdInAndStatusAndCorrectTrue(
            String userKey,
            Collection<String> practiceItemIds,
            String status
    );
}
