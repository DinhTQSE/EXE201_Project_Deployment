package com.vsign.backend.admin.service;

import com.vsign.backend.admin.dto.AdminUserListResponse;
import com.vsign.backend.admin.dto.AdminUserResponse;
import com.vsign.backend.admin.persistence.AdminUserAccountEntity;
import com.vsign.backend.admin.persistence.AdminUserAccountRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class AdminUserService {
    private final AdminUserAccountRepository userRepository;

    public AdminUserService(AdminUserAccountRepository userRepository) {
        this.userRepository = userRepository;
    }

    public AdminUserListResponse listUsers(String role, String status) {
        List<AdminUserResponse> filtered = userRepository.findAllByOrderByIdAsc().stream()
                .filter(user -> role == null || role.isBlank() || user.getRole().equalsIgnoreCase(role))
                .filter(user -> status == null || status.isBlank() || user.getStatus().equalsIgnoreCase(status))
                .map(this::toResponse)
                .toList();
        return new AdminUserListResponse(filtered, filtered.size());
    }

    public int activeUsers() {
        return userRepository.countByStatus("ACTIVE");
    }

    public int premiumUsers() {
        return userRepository.countByAccountType("PREMIUM");
    }

    private AdminUserResponse toResponse(AdminUserAccountEntity user) {
        return new AdminUserResponse(
                user.getId(),
                user.getEmail(),
                user.getDisplayName(),
                user.getRole(),
                user.getStatus(),
                user.getAccountType(),
                user.getCreatedAt().toString()
        );
    }
}
