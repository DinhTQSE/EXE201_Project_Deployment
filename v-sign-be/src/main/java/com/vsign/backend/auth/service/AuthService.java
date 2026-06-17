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
import com.vsign.backend.auth.dto.PasswordResetCompleteRequest;
import com.vsign.backend.common.mail.EmailService;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.Locale;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
public class AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final TierRepository tierRepository;
    private final UserTierRepository userTierRepository;
    private final EmailService emailService;

    @Value("${app.password-reset.frontend-url:http://localhost:5173/reset-password}")
    private String passwordResetUrl;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder,
                       JwtService jwtService, TierRepository tierRepository,
                       UserTierRepository userTierRepository, EmailService emailService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.tierRepository = tierRepository;
        this.userTierRepository = userTierRepository;
        this.emailService = emailService;
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

        var freeTierOpt = tierRepository.findByTitleIgnoreCaseAndIsActiveTrueAndDeletedAtIsNull("free");
        if (freeTierOpt.isEmpty()) {
            log.warn("No active free tier found in database — new user {} registered without a tier", email);
        } else {
            var freeTier = freeTierOpt.get();
            LocalDateTime now = LocalDateTime.now();
            int months = freeTier.getNoMonth() != null && freeTier.getNoMonth() > 0
                    ? freeTier.getNoMonth() : 120;
            UserTierEntity userTier = new UserTierEntity();
            userTier.setUser(saved);
            userTier.setTier(freeTier);
            userTier.setStartTime(now);
            userTier.setEndTime(now.plusMonths(months));
            userTier.setIsActive(true);
            userTierRepository.save(userTier);
        }

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

    @Transactional
    public void requestPasswordReset(PasswordResetRequest request) {
        String email = normalizeEmail(request.email());
        UserEntity user = userRepository.findByEmailIgnoreCase(email).orElse(null);
        if (user != null) {
            // Generate secure random 32-byte token
            String rawToken = Base64.getUrlEncoder().withoutPadding().encodeToString(
                    UUID.randomUUID().toString().getBytes(StandardCharsets.UTF_8));
            String tokenHash = sha256(rawToken);

            user.setPwdResetTokenHash(tokenHash);
            user.setPwdResetExpiry(OffsetDateTime.now().plusMinutes(15));
            userRepository.save(user);

            // Send reset email asynchronously
            String resetLink = passwordResetUrl + "?token=" + rawToken;
            String subject = "Yêu cầu khôi phục mật khẩu V-Sign";
            String body = "<h3>Yêu cầu khôi phục mật khẩu tài khoản V-Sign</h3>" +
                    "<p>Bạn đã yêu cầu đặt lại mật khẩu cho tài khoản: <strong>" + email + "</strong></p>" +
                    "<p>Vui lòng click vào đường liên kết dưới đây để thực hiện thay đổi mật khẩu (liên kết có hiệu lực trong 15 phút):</p>" +
                    "<p><a href=\"" + resetLink + "\">" + resetLink + "</a></p>" +
                    "<p>Nếu bạn không thực hiện yêu cầu này, vui lòng bỏ qua email này.</p>";
            emailService.sendEmail(email, subject, body);
        }
    }

    @Transactional
    public void completePasswordReset(PasswordResetCompleteRequest request) {
        if (!request.newPassword().equals(request.confirmPassword())) {
            throw new FieldValidationException("confirmPassword", "Mật khẩu xác nhận không khớp.");
        }
        if (request.newPassword().length() < 8 || !request.newPassword().matches(".*[A-Z].*") || !request.newPassword().matches(".*\\d.*")) {
            throw new FieldValidationException("newPassword", "Mật khẩu tối thiểu 8 ký tự, có chữ hoa và số.");
        }

        String tokenHash = sha256(request.token());
        UserEntity user = userRepository.findByPwdResetTokenHash(tokenHash)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_TOKEN, "Liên kết đã hết hạn hoặc không hợp lệ. Vui lòng yêu cầu lại."));

        if (user.getPwdResetExpiry() == null || user.getPwdResetExpiry().isBefore(OffsetDateTime.now())) {
            throw new BusinessException(ErrorCode.TOKEN_EXPIRED, "Liên kết đã hết hạn. Vui lòng yêu cầu lại.");
        }

        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        user.setPwdResetTokenHash(null);
        user.setPwdResetExpiry(null);
        userRepository.save(user);
    }

    private String sha256(String data) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            throw new RuntimeException("SHA-256 hashing failed", e);
        }
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
