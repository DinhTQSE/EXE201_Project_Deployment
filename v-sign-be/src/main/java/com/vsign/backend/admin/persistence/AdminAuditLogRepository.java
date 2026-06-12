package com.vsign.backend.admin.persistence;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AdminAuditLogRepository extends JpaRepository<AdminAuditLogEntity, Integer> {
    List<AdminAuditLogEntity> findAllByOrderByIdDesc();
}
