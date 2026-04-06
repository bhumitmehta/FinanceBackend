package org.example.financebackend.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.financebackend.dto.request.CreateRoleRequest;
import org.example.financebackend.dto.response.RoleResponse;
import org.example.financebackend.service.RoleService;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Tag(name = "Roles", description = "Runtime role management — requires users:manage")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/roles")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('PERM_users:manage')")
public class RoleController {

    private final RoleService roleService;

    @Operation(
            summary = "List all roles",
            description = "Returns every role with its full permission list.")
    @GetMapping
    public List<RoleResponse> getAll() {
        return roleService.getAllRoles();
    }

    @Operation(summary = "Get role by ID", description = "Returns a single role with its permission list.")
    @GetMapping("/{id}")
    public RoleResponse getById(@PathVariable UUID id) {
        return roleService.getById(id);
    }

    @Operation(
            summary = "Create role",
            description = "Creates a new role and optionally assigns existing permissions by ID. " +
                          "name is lowercased automatically. Fails with 409 if name already exists.")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RoleResponse create(@RequestBody @Valid CreateRoleRequest request) {
        return roleService.create(request);
    }

    @Operation(
            summary = "Add permission to role",
            description = "Assigns an existing permission to this role at runtime. " +
                          "Fails with 409 if the permission is already present.")
    @PostMapping("/{id}/permissions/{permissionId}")
    public RoleResponse addPermission(@PathVariable UUID id,
                                      @PathVariable UUID permissionId) {
        return roleService.addPermission(id, permissionId);
    }

    @Operation(
            summary = "Remove permission from role",
            description = "Detaches a permission from this role. " +
                          "Fails with 404 if the role does not hold this permission.")
    @DeleteMapping("/{id}/permissions/{permissionId}")
    public RoleResponse removePermission(@PathVariable UUID id,
                                         @PathVariable UUID permissionId) {
        return roleService.removePermission(id, permissionId);
    }

    @Operation(
            summary = "Delete role",
            description = "Fails with 409 if any user currently holds this role — " +
                          "reassign all users to a different role first.")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        roleService.delete(id);
    }
}
