package org.example.financebackend.dto.request;

import jakarta.validation.constraints.NotNull;
import org.example.financebackend.model.UserStatus;

public record ToggleStatusRequest(
        @NotNull(message = "Status is required")
        UserStatus status
) {}
