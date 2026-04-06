package org.example.financebackend.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.financebackend.dto.request.RegisterRequest;
import org.example.financebackend.dto.request.ToggleStatusRequest;
import org.example.financebackend.dto.response.PermissionResponse;
import org.example.financebackend.dto.response.UserResponse;
import org.example.financebackend.dto.response.UserRoleHistoryResponse;
import org.example.financebackend.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Tag(name = "User Management", description = "Requires users:manage permission")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('PERM_users:manage')")
public class UserController {

    private final UserService userService;

    @Operation(summary = "List all users", description = "Returns every user in the system")
    @GetMapping
    public List<UserResponse> getAllUsers() {
        return userService.getAllUsers();
    }

    @Operation(summary = "Create user", description = "Admin creates a new user account")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse createUser(@RequestBody @Valid RegisterRequest request) {
        return userService.createUser(request);
    }

    // ── Multi-role assignment ─────────────────────────────────────────────────

    @Operation(
            summary = "Assign role to user",
            description = "Appends a role to the user's role set (multi-role supported). " +
                          "Publishes RoleAssignedEvent → persists to user_role_history. " +
                          "Fails with 409 if user already holds this role.")
    @PostMapping("/{id}/roles/{roleId}")
    public UserResponse assignRole(@PathVariable UUID id,
                                   @PathVariable UUID roleId,
                                   @RequestParam(required = false) String reason) {
        return userService.assignRole(id, roleId, reason);
    }

    @Operation(
            summary = "Revoke role from user",
            description = "Removes a role from the user's role set. " +
                          "Publishes RoleRevokedEvent → persists to user_role_history. " +
                          "Fails with 400 if this is the user's last remaining role.")
    @DeleteMapping("/{id}/roles/{roleId}")
    public UserResponse revokeRole(@PathVariable UUID id,
                                   @PathVariable UUID roleId,
                                   @RequestParam(required = false) String reason) {
        return userService.revokeRole(id, roleId, reason);
    }

    @Operation(
            summary = "Get effective permissions",
            description = "Returns the deduplicated union of all permissions across all roles the user holds. " +
                          "Composite pattern: User → roles → permissions (flattened).")
    @GetMapping("/{id}/permissions")
    public List<PermissionResponse> getUserPermissions(@PathVariable UUID id) {
        return userService.getUserPermissions(id);
    }

    // ── Status + history ──────────────────────────────────────────────────────

    @Operation(summary = "Toggle status", description = "Activate or suspend a user account")
    @PatchMapping("/{id}/status")
    public UserResponse toggleStatus(@PathVariable UUID id,
                                     @RequestBody @Valid ToggleStatusRequest request) {
        return userService.toggleStatus(id, request);
    }

    @Operation(
            summary = "Role change history",
            description = "Full audit log of role assignments and revocations for a user, sorted by changedAt desc. " +
                          "previousRole is null for assignments; newRole is null for revocations.")
    @GetMapping("/{id}/role-history")
    public List<UserRoleHistoryResponse> getRoleHistory(@PathVariable UUID id) {
        return userService.getRoleHistory(id);
    }
}


