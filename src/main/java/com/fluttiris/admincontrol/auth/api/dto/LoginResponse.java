package com.fluttiris.admincontrol.auth.api.dto;

public record LoginResponse(String accessToken, long expiresIn) {
}
