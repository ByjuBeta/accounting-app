package com.accountingapp.auth.dto;

import com.accountingapp.user.Role;
import java.util.UUID;

public record MembershipDto(UUID organizationId, String organizationName, Role role) {
}
