package com.gameflix.gameflix.auth;

public record TokenResponse(String accessToken, String tokenType, long expiresIn) {}
