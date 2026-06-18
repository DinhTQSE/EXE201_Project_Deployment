package com.vsign.backend.auth.service;

import com.vsign.backend.auth.persistence.UserEntity;
import com.vsign.backend.auth.persistence.UserRepository;
import com.vsign.backend.common.exception.BusinessException;
import com.vsign.backend.common.exception.ErrorCode;
import com.vsign.backend.common.mail.EmailService;
import com.vsign.backend.common.security.JwtService;
import com.vsign.backend.payment.persistence.TierRepository;
import com.vsign.backend.payment.persistence.UserTierEntity;
import com.vsign.backend.payment.persistence.UserTierRepository;
import com.vsign.backend.gamification.persistence.GamificationProfileEntity;
import com.vsign.backend.gamification.persistence.GamificationProfileRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
public class GoogleOAuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final TierRepository tierRepository;
    private final UserTierRepository userTierRepository;
    private final EmailService emailService;
    private final RestTemplate restTemplate;
    private final GamificationProfileRepository gamificationProfileRepository;

    @Value("${spring.security.oauth2.client.registration.google.client-id}")
    private String clientId;

    @Value("${spring.security.oauth2.client.registration.google.client-secret}")
    private String clientSecret;

    @Value("${google.redirect-uri:http://localhost:8080/V-sign/api/v1/auth/google/callback}")
    private String redirectUri;

    @Value("${app.oauth2.frontend-success-url:http://localhost:5173/home}")
    private String oauthSuccessUrl;

    @Value("${app.oauth2.frontend-failure-url:http://localhost:5173/}")
    private String oauthFailureUrl;

    public GoogleOAuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            TierRepository tierRepository,
            UserTierRepository userTierRepository,
            EmailService emailService,
            GamificationProfileRepository gamificationProfileRepository) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.tierRepository = tierRepository;
        this.userTierRepository = userTierRepository;
        this.emailService = emailService;
        this.gamificationProfileRepository = gamificationProfileRepository;
        this.restTemplate = new RestTemplate();
    }

    public String getAuthorizationUrl() {
        return "https://accounts.google.com/o/oauth2/v2/auth" +
                "?client_id=" + URLEncoder.encode(clientId, StandardCharsets.UTF_8) +
                "&redirect_uri=" + URLEncoder.encode(redirectUri, StandardCharsets.UTF_8) +
                "&response_type=code" +
                "&scope=" + URLEncoder.encode("openid email profile", StandardCharsets.UTF_8);
    }

    @Transactional
    public String handleCallbackAndGetRedirect(String code) {
        try {
            // 1. Exchange authorization code for token
            String accessToken = exchangeCodeForAccessToken(code);

            // 2. Fetch User profile using token
            Map<String, Object> profile = fetchUserProfile(accessToken);

            String email = (String) profile.get("email");
            String name = (String) profile.get("name");
            String picture = (String) profile.get("picture");

            if (email == null || email.isBlank()) {
                throw new IllegalArgumentException("Email not provided by Google");
            }

            email = email.trim().toLowerCase(Locale.ROOT);

            // 3. Check existing user or register
            UserEntity user = userRepository.findByEmailIgnoreCase(email).orElse(null);
            if (user != null) {
                if (!user.isActive()) {
                    return oauthFailureUrl + "?error=account_disabled";
                }
            } else {
                user = registerGoogleUser(email, name, picture);
            }

            // 4. Generate JWT tokens
            String jwtToken = jwtService.generateToken(user.getEmail(), user.getRole());

            return oauthSuccessUrl +
                    "?accessToken=" + URLEncoder.encode(jwtToken, StandardCharsets.UTF_8) +
                    "&tokenType=Bearer" +
                    "&expiresIn=3600" +
                    "&email=" + URLEncoder.encode(user.getEmail(), StandardCharsets.UTF_8);

        } catch (Exception e) {
            log.error("Google OAuth login failed", e);
            return oauthFailureUrl + "?error=oauth_failed";
        }
    }

    private String exchangeCodeForAccessToken(String code) {
        String url = "https://oauth2.googleapis.com/token";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("code", code);
        body.add("client_id", clientId);
        body.add("client_secret", clientSecret);
        body.add("redirect_uri", redirectUri);
        body.add("grant_type", "authorization_code");

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);

        ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);
        if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
            return (String) response.getBody().get("access_token");
        }
        throw new IllegalStateException("Failed to exchange code for tokens");
    }

    private Map<String, Object> fetchUserProfile(String accessToken) {
        String url = "https://www.googleapis.com/oauth2/v3/userinfo";

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);

        HttpEntity<Void> entity = new HttpEntity<>(headers);

        ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, entity, Map.class);
        if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
            return response.getBody();
        }
        throw new IllegalStateException("Failed to fetch Google user profile");
    }

    private UserEntity registerGoogleUser(String email, String name, String avatarUrl) {
        String rawPassword = "HT-GOOGLE-" + UUID.randomUUID().toString().substring(0, 12);

        UserEntity user = new UserEntity();
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(rawPassword));
        user.setFullName(name != null && !name.isBlank() ? name.trim() : email.split("@")[0]);
        user.setAvatarUrl(avatarUrl);
        user.setAccountType("BASIC");
        user.setRole("USER");
        user.setActive(true);

        UserEntity saved = userRepository.save(user);

        // Create gamification profile
        gamificationProfileRepository.save(new GamificationProfileEntity(
                email,
                "user-" + Integer.toUnsignedString(email.hashCode()),
                saved.getFullName()
        ));

        // Seed default free tier
        var freeTierOpt = tierRepository.findByTitleIgnoreCaseAndIsActiveTrueAndDeletedAtIsNull("free");
        if (freeTierOpt.isPresent()) {
            var freeTier = freeTierOpt.get();
            LocalDateTime now = LocalDateTime.now();
            int months = freeTier.getNoMonth() != null && freeTier.getNoMonth() > 0 ? freeTier.getNoMonth() : 120;
            UserTierEntity userTier = new UserTierEntity();
            userTier.setUser(saved);
            userTier.setTier(freeTier);
            userTier.setStartTime(now);
            userTier.setEndTime(now.plusMonths(months));
            userTier.setIsActive(true);
            userTierRepository.save(userTier);
        }

        // Asynchronously send temp credentials email
        String emailSubject = "Chào mừng bạn đến với V-Sign!";
        String emailBody = "<h3>Tài khoản của bạn đã được khởi tạo thành công qua Google OAuth</h3>" +
                "<p>Dưới đây là thông tin mật khẩu tạm thời của bạn để bạn có thể đăng nhập thủ công:</p>" +
                "<p><strong>Mật khẩu:</strong> " + rawPassword + "</p>" +
                "<p>Vui lòng đổi mật khẩu tại trang Cá nhân sau khi đăng nhập để đảm bảo an toàn.</p>";
        emailService.sendEmail(email, emailSubject, emailBody);

        return saved;
    }
}
