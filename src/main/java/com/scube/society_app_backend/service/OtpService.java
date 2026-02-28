package com.scube.society_app_backend.service;

import com.scube.society_app_backend.config.AuthProperties;
import com.scube.society_app_backend.entity.OtpVerification;
import com.scube.society_app_backend.repository.OtpVerificationRepository;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.concurrent.TimeUnit;

/**
 * Generates OTP, persists it via {@link OtpVerificationRepository}, and sends via {@link SmsSender}.
 */
@Service
public class OtpService {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final AuthProperties authProperties;
    private final OtpVerificationRepository otpVerificationRepository;
    private final SmsSender smsSender;

    public OtpService(AuthProperties authProperties,
                      OtpVerificationRepository otpVerificationRepository,
                      SmsSender smsSender) {
        this.authProperties = authProperties;
        this.otpVerificationRepository = otpVerificationRepository;
        this.smsSender = smsSender;
    }

    /**
     * Generate a numeric OTP of configured length (e.g. 6 digits).
     */
    public String generateCode() {
        int length = Math.max(4, Math.min(8, authProperties.getOtp().getLength()));
        int max = (int) Math.pow(10, length);
        int value = RANDOM.nextInt(max);
        return String.format("%0" + length + "d", value);
    }

    /**
     * Create OTP for phone: generate code, save to DB, and send via SMS (or log).
     * Phone is normalized (trim, strip leading +).
     */
    public void createAndSend(String phone) {
        String normalized = normalizePhone(phone);
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("phone required");
        }

        String code = generateCode();
        int expiryMin = authProperties.getOtp().getExpiryMin();
        Instant expiresAt = Instant.now().plus(expiryMin, TimeUnit.MINUTES.toChronoUnit());

        OtpVerification otp = new OtpVerification();
        otp.setPhone(normalized);
        otp.setCode(code);
        otp.setExpiresAt(expiresAt);
        otpVerificationRepository.save(otp);

        smsSender.sendOtp(normalized, code, expiryMin);
    }

    private static String normalizePhone(String phone) {
        if (phone == null) return "";
        String s = phone.trim();
        if (s.startsWith("+")) s = s.substring(1).trim();
        return s;
    }
}
