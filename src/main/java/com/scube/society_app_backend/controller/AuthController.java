package com.scube.society_app_backend.controller;

import com.scube.society_app_backend.dto.*;
import com.scube.society_app_backend.entity.User;
import com.scube.society_app_backend.repository.UserRepository;
import com.scube.society_app_backend.security.JwtAuthFilter;
import com.scube.society_app_backend.service.AuthService;
import com.scube.society_app_backend.service.OtpService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final OtpService otpService;
    private final AuthService authService;
    private final UserRepository userRepository;

    public AuthController(OtpService otpService, AuthService authService, UserRepository userRepository) {
        this.otpService = otpService;
        this.authService = authService;
        this.userRepository = userRepository;
    }

    @PostMapping("/otp/request")
    public ResponseEntity<Map<String, String>> requestOtp(@RequestBody OtpRequest request) {
        if (request == null || request.phone() == null || request.phone().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "phone required"));
        }
        try {
            otpService.createAndSend(request.phone());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
        return ResponseEntity.ok(Map.of("message", "otp_sent"));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        if (request == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "invalid body"));
        }
        try {
            LoginResponse response = authService.login(request.phone(), request.otp());
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            String msg = e.getMessage();
            if ("invalid_or_expired_otp".equals(msg) || "phone and otp required".equals(msg)) {
                return ResponseEntity.status(401).body(Map.of("error", msg));
            }
            return ResponseEntity.badRequest().body(Map.of("error", msg));
        }
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(@RequestBody RefreshRequest request) {
        if (request == null || request.refresh_token() == null || request.refresh_token().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "refresh_token required"));
        }
        try {
            LoginResponse response = authService.refresh(request.refresh_token());
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(401).body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/me")
    public ResponseEntity<Object> me(HttpServletRequest httpRequest) {
        UUID userId = (UUID) httpRequest.getAttribute(JwtAuthFilter.REQUEST_ATTR_USER_ID);
        if (userId == null) {
            return ResponseEntity.status(401).body((Object) Map.of("error", "unauthorized"));
        }
        return userRepository.findById(userId)
                .map(user -> ResponseEntity.<Object>ok(AuthService.toUserResponse(user)))
                .orElse(ResponseEntity.status(404).body((Object) Map.of("error", "user not found")));
    }

    @PatchMapping("/profile")
    public ResponseEntity<?> updateProfile(@RequestBody UpdateProfileRequest body, HttpServletRequest httpRequest) {
        UUID userId = (UUID) httpRequest.getAttribute(JwtAuthFilter.REQUEST_ATTR_USER_ID);
        if (userId == null) {
            return ResponseEntity.status(401).body(Map.of("error", "unauthorized"));
        }
        if (body == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "invalid body"));
        }
        String first = body.first_name() != null ? body.first_name().trim() : "";
        String last = body.last_name() != null ? body.last_name().trim() : "";
        if (first.isEmpty() && last.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "first_name and last_name required"));
        }
        String fullName = (first + " " + last).trim();
        return userRepository.findById(userId)
                .map(user -> {
                    user.setName(fullName);
                    User saved = userRepository.save(user);
                    return ResponseEntity.<Object>ok(AuthService.toUserResponse(saved));
                })
                .orElse(ResponseEntity.status(404).body((Object) Map.of("error", "user not found")));
    }
}
