package org.example.financebackend.dto.response;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

public record RoleResponse(
        UUID id,
        String name,
        String description,
        Set<PermissionResponse> permissions,
        LocalDateTime createdAt
) {}
