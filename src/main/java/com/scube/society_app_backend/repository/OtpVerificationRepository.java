package com.scube.society_app_backend.repository;

import com.scube.society_app_backend.entity.OtpVerification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface OtpVerificationRepository extends JpaRepository<OtpVerification, UUID> {

    Optional<OtpVerification> findByPhoneAndCodeAndUsedAtIsNullAndExpiresAtAfter(
            String phone, String code, Instant now);
}
