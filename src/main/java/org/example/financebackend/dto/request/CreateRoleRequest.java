package org.example.financebackend.dto.request;

import jakarta.validation.constraints.NotBlank;

import java.util.List;
import java.util.UUID;

public record CreateRoleRequest(

        @NotBlank(message = "name is required")
        String name,

        String description,

        /** Permission IDs to assign on creation. May be empty but not null. */
        List<UUID> permissionIds
) {}
