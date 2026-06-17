package com.vsign.backend.auth.service;

import com.vsign.backend.auth.dto.AuthResponse;
import com.vsign.backend.auth.dto.ChangePasswordRequest;
import com.vsign.backend.auth.dto.LoginRequest;
import com.vsign.backend.auth.dto.PasswordResetRequest;
import com.vsign.backend.auth.dto.RegisterRequest;
import com.vsign.backend.auth.persistence.UserEntity;
import com.vsign.backend.auth.persistence.UserRepository;
import com.vsign.backend.common.exception.BusinessException;
import com.vsign.backend.common.exception.ErrorCode;
import com.vsign.backend.common.exception.FieldValidationException;
import com.vsign.backend.common.security.JwtService;
import com.vsign.backend.payment.persistence.TierRepository;
import com.vsign.backend.payment.persistence.UserTierEntity;
import com.vsign.backend.payment.persistence.UserTierRepository;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.Locale;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final TierRepository tierRepository;
    private final UserTierRepository userTierRepository;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder,
                       JwtService jwtService, TierRepository tierRepository,
                       UserTierRepository userTierRepository) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.tierRepository = tierRepository;
        this.userTierRepository = userTierRepository;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String email = normalizeEmail(request.email());
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new BusinessException(ErrorCode.EMAIL_ALREADY_EXISTS, "Email này đã được sử dụng.");
        }
        String resolvedName = request.resolvedName();
        if (resolvedName == null || resolvedName.isBlank()) {
            throw new FieldValidationException("displayName", "Display name is required");
        }

        UserEntity user = new UserEntity();
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setFullName(resolvedName.trim());
        user.setAccountType("BASIC");
        user.setRole("USER");
        user.setActive(true);

        UserEntity saved = userRepository.save(user);

        tierRepository.findByTitleIgnoreCaseAndIsActiveTrueAndDeletedAtIsNull("free")
                .ifPresent(freeTier -> {
                    UserTierEntity userTier = new UserTierEntity();
                    userTier.setUser(saved);
                    userTier.setTier(freeTier);
                    userTier.setStartTime(LocalDateTime.now());
                    int months = freeTier.getNoMonth() != null && freeTier.getNoMonth() > 0
                            ? freeTier.getNoMonth() : 120;
                    userTier.setEndTime(LocalDateTime.now().plusMonths(months));
                    userTier.setIsActive(true);
                    userTierRepository.save(userTier);
                });

        return toAuthResponse(saved);
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        UserEntity user = userRepository.findByEmailIgnoreCase(normalizeEmail(request.email()))
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_CREDENTIALS, "Tài khoản không tồn tại."));

        if (!user.isActive()) {
            throw new BusinessException(ErrorCode.ACCOUNT_DISABLED, "Tài khoản đã bị khóa. Liên hệ hỗ trợ.");
        }
        if (user.getPasswordHash() == null || !passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new BusinessException(ErrorCode.INVALID_CREDENTIALS, "Sai mật khẩu");
        }

        user.setLastSeenAt(OffsetDateTime.now());
        userRepository.save(user);
        return toAuthResponse(user);
    }

    @Transactional
    public void changePassword(String email, ChangePasswordRequest request) {
        UserEntity user = userRepository.findByEmailIgnoreCase(normalizeEmail(email))
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            throw new BusinessException(ErrorCode.INVALID_CREDENTIALS, "Mật khẩu hiện tại không đúng.");
        }
        if (passwordEncoder.matches(request.newPassword(), user.getPasswordHash())) {
            throw new FieldValidationException("newPassword", "Mật khẩu mới phải khác mật khẩu cũ.");
        }
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);
    }

    public void requestPasswordReset(PasswordResetRequest request) {
        // Intentionally no-op in this FE-first phase to avoid account enumeration.
    }

    private AuthResponse toAuthResponse(UserEntity user) {
        return new AuthResponse(
                jwtService.generateToken(user.getEmail(), user.getRole()),
                "Bearer",
                new AuthResponse.AuthUserResponse(
                        user.getId().toString(),
                        user.getEmail(),
                        user.getFullName(),
                        user.getFullName(),
                        user.getAvatarUrl(),
                        "",
                        user.getRole(),
                        user.getAccountType()
                )
        );
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
