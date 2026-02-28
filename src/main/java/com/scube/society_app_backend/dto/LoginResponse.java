package com.scube.society_app_backend.dto;

public record LoginResponse(String access_token, String refresh_token, long expires_in, UserResponse user) {}
