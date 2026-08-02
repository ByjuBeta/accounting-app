package com.accountingapp.user.dto;

import com.accountingapp.user.Role;
import jakarta.validation.constraints.NotNull;

public record UpdateMemberRoleRequest(@NotNull Role role) {
}
