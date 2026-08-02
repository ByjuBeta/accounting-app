package com.accountingapp.user.dto;

import com.accountingapp.user.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record AddMemberRequest(@NotBlank @Email String email, @NotNull Role role) {
}
