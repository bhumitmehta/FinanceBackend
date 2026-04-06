package org.example.financebackend.dto.request;

import jakarta.validation.constraints.NotNull;
import org.example.financebackend.model.RoleType;

public record AssignRoleRequest(
        @NotNull(message = "Role is required")
        RoleType role,
        String reason
) {}
