package org.example.financebackend.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

public record PermissionResponse(
        UUID id,
        String name,
        String resource,
        String action,
        String description,
        LocalDateTime createdAt
) {}
