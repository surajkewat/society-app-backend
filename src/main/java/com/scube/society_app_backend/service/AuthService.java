package com.scube.society_app_backend.service;

import com.scube.society_app_backend.dto.LoginResponse;
import com.scube.society_app_backend.dto.UserResponse;
import com.scube.society_app_backend.entity.OtpVerification;
import com.scube.society_app_backend.entity.RefreshToken;
import com.scube.society_app_backend.entity.User;
import com.scube.society_app_backend.repository.OtpVerificationRepository;
import com.scube.society_app_backend.repository.RefreshTokenRepository;
import com.scube.society_app_backend.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final OtpVerificationRepository otpVerificationRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtService jwtService;

    public AuthService(UserRepository userRepository,
                       OtpVerificationRepository otpVerificationRepository,
                       RefreshTokenRepository refreshTokenRepository,
                       JwtService jwtService) {
        this.userRepository = userRepository;
        this.otpVerificationRepository = otpVerificationRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.jwtService = jwtService;
    }

    @Transactional
    public LoginResponse login(String phone, String otp) {
        String normalizedPhone = normalizePhone(phone);
        String code = otp != null ? otp.trim() : "";
        if (normalizedPhone.isEmpty() || code.isEmpty()) {
            throw new IllegalArgumentException("phone and otp required");
        }

        Optional<OtpVerification> otpOpt = otpVerificationRepository
                .findByPhoneAndCodeAndUsedAtIsNullAndExpiresAtAfter(normalizedPhone, code, Instant.now());
        OtpVerification otpRow = otpOpt.orElseThrow(() -> new IllegalArgumentException("invalid_or_expired_otp"));

        otpRow.setUsedAt(Instant.now());
        otpVerificationRepository.save(otpRow);

        User user = userRepository.findByPhone(normalizedPhone)
                .orElseGet(() -> createUserByPhone(normalizedPhone));

        String accessToken = jwtService.issueAccessToken(user.getId());
        JwtService.RefreshTokenData refreshData = jwtService.createRefreshToken();

        RefreshToken refreshEntity = new RefreshToken();
        refreshEntity.setUserId(user.getId());
        refreshEntity.setTokenHash(refreshData.tokenHash());
        refreshEntity.setExpiresAt(refreshData.expiresAt());
        refreshTokenRepository.save(refreshEntity);

        return new LoginResponse(
                accessToken,
                refreshData.rawToken(),
                jwtService.getAccessTtlSeconds(),
                toUserResponse(user)
        );
    }

    @Transactional
    public LoginResponse refresh(String refreshTokenRaw) {
        String raw = refreshTokenRaw != null ? refreshTokenRaw.trim() : "";
        if (raw.isEmpty()) {
            throw new IllegalArgumentException("refresh_token required");
        }

        String hash = JwtService.hashRefreshToken(raw);
        Optional<RefreshToken> tokenOpt = refreshTokenRepository
                .findByTokenHashAndRevokedAtIsNullAndExpiresAtAfter(hash, Instant.now());
        RefreshToken token = tokenOpt.orElseThrow(() -> new IllegalArgumentException("invalid_or_expired_refresh_token"));

        token.setRevokedAt(Instant.now());
        refreshTokenRepository.save(token);

        String accessToken = jwtService.issueAccessToken(token.getUserId());
        JwtService.RefreshTokenData newRefresh = jwtService.createRefreshToken();

        RefreshToken newEntity = new RefreshToken();
        newEntity.setUserId(token.getUserId());
        newEntity.setTokenHash(newRefresh.tokenHash());
        newEntity.setExpiresAt(newRefresh.expiresAt());
        refreshTokenRepository.save(newEntity);

        User user = userRepository.findById(token.getUserId()).orElseThrow();
        return new LoginResponse(
                accessToken,
                newRefresh.rawToken(),
                jwtService.getAccessTtlSeconds(),
                toUserResponse(user)
        );
    }

    private User createUserByPhone(String phone) {
        User user = new User();
        user.setPhone(phone);
        user.setName("");
        return userRepository.save(user);
    }

    private static String normalizePhone(String phone) {
        if (phone == null) return "";
        String s = phone.trim();
        if (s.startsWith("+")) s = s.substring(1).trim();
        return s;
    }

    public static UserResponse toUserResponse(User user) {
        return new UserResponse(
                user.getId().toString(),
                user.getPhone() != null ? user.getPhone() : "",
                user.getName() != null ? user.getName() : ""
        );
    }
}
