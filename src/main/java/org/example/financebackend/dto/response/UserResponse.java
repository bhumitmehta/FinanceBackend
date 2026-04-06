package org.example.financebackend.dto.response;

import org.example.financebackend.model.UserStatus;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

public record UserResponse(
        UUID id,
        String name,
        String email,
        Set<String> roles,
        UserStatus status,
        LocalDateTime createdAt
) {}
