package org.example.financebackend.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record CreatePermissionRequest(

        /**
         * Must follow the {@code resource:action} convention, e.g. {@code "invoices:approve"}.
         * Only lowercase letters and colons are permitted; exactly one colon required.
         */
        @NotBlank(message = "name is required")
        @Pattern(
                regexp = "^[a-z]+:[a-z]+$",
                message = "name must follow resource:action format (lowercase letters only, e.g. 'records:write')"
        )
        String name,

        @NotBlank(message = "resource is required")
        String resource,

        @NotBlank(message = "action is required")
        String action,

        String description
) {}
