package org.example.financebackend.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.financebackend.dto.request.CreatePermissionRequest;
import org.example.financebackend.dto.response.PermissionResponse;
import org.example.financebackend.service.PermissionService;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Tag(name = "Permissions", description = "Runtime permission management — requires users:manage")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/permissions")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('PERM_users:manage')")
public class PermissionController {

    private final PermissionService permissionService;

    @Operation(
            summary = "List all permissions",
            description = "Returns every permission defined in the system, ordered by name.")
    @GetMapping
    public List<PermissionResponse> getAll() {
        return permissionService.getAllPermissions();
    }

    @Operation(summary = "Get permission by ID")
    @GetMapping("/{id}")
    public PermissionResponse getById(@PathVariable UUID id) {
        return permissionService.getById(id);
    }

    @Operation(
            summary = "Create permission",
            description = "name must follow resource:action format (lowercase, e.g. 'invoices:approve'). " +
                          "name must equal resource + ':' + action. Fails with 409 if name already exists.")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PermissionResponse create(@RequestBody @Valid CreatePermissionRequest request) {
        return permissionService.create(request);
    }

    @Operation(
            summary = "Delete permission",
            description = "Fails with 409 if any role currently holds this permission — " +
                          "remove the permission from all roles first.")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        permissionService.delete(id);
    }
}
