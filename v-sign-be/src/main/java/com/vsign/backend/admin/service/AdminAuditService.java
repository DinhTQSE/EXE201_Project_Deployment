package com.vsign.backend.admin.service;

import com.vsign.backend.admin.dto.AdminAuditLogResponse;
import com.vsign.backend.admin.persistence.AdminAuditLogEntity;
import com.vsign.backend.admin.persistence.AdminAuditLogRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class AdminAuditService {
    private final AdminAuditLogRepository auditLogRepository;

    public AdminAuditService(AdminAuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @Transactional
    public AdminAuditLogResponse recordAction(
            String actorEmail,
            String action,
            String targetType,
            String targetId,
            String reason
    ) {
        return toResponse(auditLogRepository.save(new AdminAuditLogEntity(
                actorEmail,
                action,
                targetType,
                targetId,
                reason
        )));
    }

    public List<AdminAuditLogResponse> list() {
        return auditLogRepository.findAllByOrderByIdDesc().stream()
                .map(this::toResponse)
                .toList();
    }

    private AdminAuditLogResponse toResponse(AdminAuditLogEntity log) {
        return new AdminAuditLogResponse(
                "audit-" + log.getId(),
                log.getActorEmail(),
                log.getAction(),
                log.getTargetType(),
                log.getTargetId(),
                log.getReason(),
                log.getCreatedAt().toString()
        );
    }
}
