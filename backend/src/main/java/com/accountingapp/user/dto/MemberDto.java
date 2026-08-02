package com.accountingapp.user.dto;

import com.accountingapp.user.Role;
import java.util.UUID;

public record MemberDto(
        UUID id, UUID userId, String email, String firstName, String lastName, Role role, boolean active) {
}
