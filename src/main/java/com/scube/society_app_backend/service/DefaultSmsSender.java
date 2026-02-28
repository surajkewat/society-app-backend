package com.scube.society_app_backend.service;

import com.scube.society_app_backend.config.AuthProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

/**
 * Sends OTP via 2Factor.in, or logs only when not configured.
 */
@Component
public class DefaultSmsSender implements SmsSender {

    private static final Logger log = LoggerFactory.getLogger(DefaultSmsSender.class);
    private static final String TWO_FACTOR_BASE = "https://2factor.in/API/V1";

    private final AuthProperties authProperties;
    private final RestTemplate restTemplate;

    public DefaultSmsSender(AuthProperties authProperties, RestTemplate restTemplate) {
        this.authProperties = authProperties;
        this.restTemplate = restTemplate;
    }

    @Override
    public void sendOtp(String phone, String code, int expiryMin) {
        AuthProperties.Sms sms = authProperties.getSms();
        if (sms.isTwoFactorConfigured()) {
            try {
                sendVia2Factor(phone, code, sms);
            } catch (Exception e) {
                log.warn("[OTP] 2Factor send failed: {}; falling back to log. phone={} code={}", e.getMessage(), phone, code);
                logOtp(phone, code, expiryMin);
            }
            return;
        }
        logOtp(phone, code, expiryMin);
    }

    private void sendVia2Factor(String phone, String code, AuthProperties.Sms sms) {
        String mobile = formatMobileIndia(phone);
        String url = TWO_FACTOR_BASE + "/" + sms.getTwoFactorApiKey() + "/SMS/" + mobile + "/" + code;

        ResponseEntity<String> response = restTemplate.postForEntity(url, null, String.class);
        if (response.getStatusCode() != HttpStatus.OK) {
            throw new RuntimeException("2Factor api: status " + response.getStatusCode());
        }
    }

    private void logOtp(String phone, String code, int expiryMin) {
        log.info("[OTP] phone={} code={} (valid for {} min) — set TWO_FACTOR_API_KEY to send SMS",
                phone, code, expiryMin);
    }

    /** Digits only; 10-digit Indian number gets 91 prefix. */
    private static String formatMobileIndia(String phone) {
        String digits = phone != null ? phone.replaceAll("\\D", "") : "";
        if (digits.length() == 10 && "6789".indexOf(digits.charAt(0)) >= 0) {
            return "91" + digits;
        }
        if (digits.length() >= 12 && digits.startsWith("91")) {
            return digits;
        }
        return digits.length() >= 10 ? digits : "91" + digits;
    }
}
