package com.scube.society_app_backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "app.auth")
public class AuthProperties {

    private final Jwt jwt = new Jwt();
    private final Otp otp = new Otp();
    private final Sms sms = new Sms();

    public Jwt getJwt() { return jwt; }
    public Otp getOtp() { return otp; }
    public Sms getSms() { return sms; }

    public static class Jwt {
        private String secret = "change-me-in-production-min-32-chars";
        private int accessTtlMin = 15;
        private int refreshTtlDays = 7;

        public String getSecret() { return secret; }
        public void setSecret(String secret) { this.secret = secret; }
        public int getAccessTtlMin() { return accessTtlMin; }
        public void setAccessTtlMin(int accessTtlMin) { this.accessTtlMin = accessTtlMin; }
        public int getRefreshTtlDays() { return refreshTtlDays; }
        public void setRefreshTtlDays(int refreshTtlDays) { this.refreshTtlDays = refreshTtlDays; }
    }

    public static class Otp {
        private int length = 6;
        private int expiryMin = 10;

        public int getLength() { return length; }
        public void setLength(int length) { this.length = length; }
        public int getExpiryMin() { return expiryMin; }
        public void setExpiryMin(int expiryMin) { this.expiryMin = expiryMin; }
    }

    public static class Sms {
        private String twoFactorApiKey = "";
        /** Delivery: "sms", "voice", or "both". 2Factor SMS = text message, voice = phone call that speaks OTP. */
        private String delivery = "sms";

        public String getTwoFactorApiKey() { return twoFactorApiKey; }
        public void setTwoFactorApiKey(String twoFactorApiKey) { this.twoFactorApiKey = twoFactorApiKey != null ? twoFactorApiKey : ""; }
        public String getDelivery() { return delivery; }
        public void setDelivery(String delivery) { this.delivery = delivery != null ? delivery : "sms"; }

        public boolean isTwoFactorConfigured() { return twoFactorApiKey != null && !twoFactorApiKey.isBlank(); }
        public boolean sendSms() { return "sms".equalsIgnoreCase(delivery) || "both".equalsIgnoreCase(delivery); }
        public boolean sendVoice() { return "voice".equalsIgnoreCase(delivery) || "both".equalsIgnoreCase(delivery); }
    }
}
