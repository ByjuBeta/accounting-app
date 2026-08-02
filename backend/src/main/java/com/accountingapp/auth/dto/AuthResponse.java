package com.accountingapp.auth.dto;

import java.util.List;

public record AuthResponse(
        String accessToken,
        String refreshToken,
        long expiresInSeconds,
        UserDto user,
        List<MembershipDto> memberships) {
}
