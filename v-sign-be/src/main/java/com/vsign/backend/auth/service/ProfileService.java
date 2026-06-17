package com.vsign.backend.auth.service;

import com.vsign.backend.auth.dto.ProfileResponse;
import com.vsign.backend.auth.dto.ChangePasswordRequest;
import com.vsign.backend.auth.dto.UpdateProfileRequest;
import com.vsign.backend.auth.persistence.UserEntity;
import com.vsign.backend.auth.persistence.UserRepository;
import com.vsign.backend.common.exception.BusinessException;
import com.vsign.backend.common.exception.ErrorCode;
import com.vsign.backend.common.exception.FieldValidationException;
import com.vsign.backend.payment.persistence.UserTierRepository;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProfileService {
    private final UserRepository userRepository;
    private final AuthService authService;
    private final UserTierRepository userTierRepository;

    public ProfileService(UserRepository userRepository, AuthService authService, UserTierRepository userTierRepository) {
        this.userRepository = userRepository;
        this.authService = authService;
        this.userTierRepository = userTierRepository;
    }

    @Transactional
    public void changePassword(String email, ChangePasswordRequest request) {
        authService.changePassword(email, request);
    }

    @Transactional(readOnly = true)
    public ProfileResponse getProfile(String email) {
        return userRepository.findByEmailIgnoreCase(email)
                .map(this::toResponse)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
    }

    @Transactional
    public ProfileResponse updateProfile(String email, UpdateProfileRequest request) {
        UserEntity user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        String resolvedName = request.resolvedName();
        if (resolvedName == null || resolvedName.isBlank()) {
            throw new FieldValidationException("fullName", "Full name is required");
        }
        user.setFullName(resolvedName.trim());
        user.setAvatarUrl(request.avatarUrl() == null || request.avatarUrl().isBlank() ? null : request.avatarUrl().trim());
        return toResponse(userRepository.save(user));
    }

    private ProfileResponse toResponse(UserEntity user) {
        var activeTiers = userTierRepository.findCurrentActiveByUserId(user.getId(), LocalDateTime.now());
        ProfileResponse.SubscriptionSummaryResponse subscriptionResponse;
        if (activeTiers.isEmpty()) {
            subscriptionResponse = new ProfileResponse.SubscriptionSummaryResponse("FREE", "INACTIVE", null, null);
        } else {
            var activeTier = activeTiers.get(0);
            subscriptionResponse = new ProfileResponse.SubscriptionSummaryResponse(
                    activeTier.getTier().getTitle().toUpperCase(),
                    "ACTIVE",
                    activeTier.getStartTime().toString(),
                    activeTier.getEndTime().toString()
            );
        }

        return new ProfileResponse(
                user.getId().toString(),
                user.getEmail(),
                user.getFullName(),
                user.getFullName(),
                user.getAvatarUrl(),
                "",
                user.getRole(),
                user.getAccountType(),
                user.getTotalXp(),
                user.getCurrentStreak(),
                user.getLongestStreak(),
                subscriptionResponse,
                java.util.List.of()
        );
    }
}
