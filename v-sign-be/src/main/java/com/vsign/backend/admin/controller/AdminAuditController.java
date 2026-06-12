package com.vsign.backend.admin.controller;

import com.vsign.backend.admin.dto.AdminAuditLogResponse;
import com.vsign.backend.admin.service.AdminAuditService;
import com.vsign.backend.common.response.SuccessResponse;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/audit-logs")
@PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
public class AdminAuditController {
    private final AdminAuditService auditService;

    public AdminAuditController(AdminAuditService auditService) {
        this.auditService = auditService;
    }

    @GetMapping
    public SuccessResponse<List<AdminAuditLogResponse>> listAuditLogs() {
        return SuccessResponse.ok("Admin audit logs loaded", auditService.list());
    }
}
