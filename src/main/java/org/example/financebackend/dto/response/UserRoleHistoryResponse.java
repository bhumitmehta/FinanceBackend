package org.example.financebackend.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

public record UserRoleHistoryResponse(
        UUID          id,
        UUID          userId,
        String        userName,
        String        previousRole,
        String        newRole,
        UUID          changedById,
        String        changedByName,
        String        reason,
        LocalDateTime changedAt
) {}
