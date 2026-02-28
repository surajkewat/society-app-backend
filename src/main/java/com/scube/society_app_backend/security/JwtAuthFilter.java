package com.scube.society_app_backend.security;

import com.scube.society_app_backend.service.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * Validates JWT for /auth/me and /auth/profile. Sets request attribute "userId" (UUID) on success; returns 401 otherwise.
 */
@Component
@Order(1)
public class JwtAuthFilter extends OncePerRequestFilter {

    public static final String REQUEST_ATTR_USER_ID = "userId";

    private static final String AUTH_ME = "/auth/me";
    private static final String AUTH_PROFILE = "/auth/profile";

    private final JwtService jwtService;

    public JwtAuthFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String path = request.getRequestURI();
        if (!path.equals(AUTH_ME) && !path.equals(AUTH_PROFILE)) {
            filterChain.doFilter(request, response);
            return;
        }

        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            sendUnauthorized(response, "missing authorization");
            return;
        }
        String token = authHeader.substring(7).trim();
        if (token.isEmpty()) {
            sendUnauthorized(response, "invalid authorization");
            return;
        }

        try {
            UUID userId = jwtService.validateAccessToken(token);
            request.setAttribute(REQUEST_ATTR_USER_ID, userId);
            filterChain.doFilter(request, response);
        } catch (Exception e) {
            sendUnauthorized(response, "invalid token");
        }
    }

    private void sendUnauthorized(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write("{\"error\":\"" + message + "\"}");
    }
}
