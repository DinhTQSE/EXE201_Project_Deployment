package com.vsign.backend.admin.controller;

import com.vsign.backend.admin.dto.AdminUserListResponse;
import com.vsign.backend.admin.service.AdminUserService;
import com.vsign.backend.common.response.SuccessResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/users")
public class AdminUserController {
    private final AdminUserService adminUserService;

    public AdminUserController(AdminUserService adminUserService) {
        this.adminUserService = adminUserService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public SuccessResponse<AdminUserListResponse> listUsers(
            @RequestParam(required = false) String role,
            @RequestParam(required = false) String status
    ) {
        return SuccessResponse.ok("Admin users loaded", adminUserService.listUsers(role, status));
    }
}
