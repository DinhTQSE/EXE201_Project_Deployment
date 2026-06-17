package com.vsign.backend.admin.controller;

import com.vsign.backend.admin.dto.AdminUserListResponse;
import com.vsign.backend.admin.dto.AdminUserDetailResponse;
import com.vsign.backend.admin.dto.AdminUserUpdateRequest;
import com.vsign.backend.admin.service.AdminUserService;
import com.vsign.backend.common.response.SuccessResponse;
import com.vsign.backend.common.security.JwtService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestBody;
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
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return SuccessResponse.ok("Admin users loaded", adminUserService.listUsers(search, role, status, page, size));
    }

    @GetMapping("/{userId}")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public SuccessResponse<AdminUserDetailResponse> getUser(@PathVariable String userId) {
        return SuccessResponse.ok("Admin user loaded", adminUserService.getUser(userId));
    }

    @GetMapping("/{userId}/activity")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public SuccessResponse<AdminUserDetailResponse> getUserActivity(@PathVariable String userId) {
        return SuccessResponse.ok("Admin user activity loaded", adminUserService.getUser(userId));
    }

    @PatchMapping("/{userId}")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public SuccessResponse<AdminUserDetailResponse> updateUser(
            @PathVariable String userId,
            @Valid @RequestBody AdminUserUpdateRequest request,
            @AuthenticationPrincipal JwtService.Principal principal
    ) {
        return SuccessResponse.ok(
                "Admin user updated",
                adminUserService.updateUser(userId, request, principal.email(), principal.role())
        );
    }

    @DeleteMapping("/{userId}")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public SuccessResponse<AdminUserDetailResponse> deactivateUser(
            @PathVariable String userId,
            @RequestParam(required = false) String reason,
            @AuthenticationPrincipal JwtService.Principal principal
    ) {
        return SuccessResponse.ok(
                "Admin user deactivated",
                adminUserService.deactivateUser(userId, principal.email(), reason)
        );
    }
}
