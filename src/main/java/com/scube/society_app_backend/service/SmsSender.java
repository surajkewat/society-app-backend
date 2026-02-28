package com.scube.society_app_backend.service;

/**
 * Sends OTP to the given phone via 2Factor.in, or logs when not configured.
 */
public interface SmsSender {

    /**
     * Send OTP to the phone. expiryMin is used in the message and for provider-specific params.
     */
    void sendOtp(String phone, String code, int expiryMin);
}
