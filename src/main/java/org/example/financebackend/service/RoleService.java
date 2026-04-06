package org.example.financebackend.service;

import lombok.RequiredArgsConstructor;
import org.example.financebackend.dto.request.CreateRoleRequest;
import org.example.financebackend.dto.response.RoleResponse;
import org.example.financebackend.exception.AppException;
import org.example.financebackend.model.Permission;
import org.example.financebackend.model.Role;
import org.example.financebackend.repository.PermissionRepository;
import org.example.financebackend.repository.RoleRepository;
import org.example.financebackend.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RoleService {

    private final RoleRepository       roleRepository;
    private final PermissionRepository permissionRepository;
    private final PermissionService    permissionService;
    private final UserRepository       userRepository;

    // ── Read ─────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<RoleResponse> getAllRoles() {
        return roleRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public RoleResponse getById(UUID id) {
        return toResponse(findById(id));
    }

    // ── Create ───────────────────────────────────────────────────────────────

    @Transactional
    public RoleResponse create(CreateRoleRequest request) {
        String name = request.name().trim().toLowerCase();

        if (roleRepository.findByName(name).isPresent()) {
            throw AppException.conflict("Role '" + name + "' already exists");
        }

        Set<Permission> permissions = resolvePermissions(request.permissionIds());

        Role role = Role.builder()
                .name(name)
                .description(request.description())
                .permissions(permissions)
                .build();

        return toResponse(roleRepository.save(role));
    }

    // ── Permission assignment ─────────────────────────────────────────────────

    @Transactional
    public RoleResponse addPermission(UUID roleId, UUID permissionId) {
        Role role = findById(roleId);
        Permission permission = findPermissionById(permissionId);

        if (role.getPermissions().stream().anyMatch(p -> p.getId().equals(permissionId))) {
            throw AppException.conflict(
                    "Role '" + role.getName() + "' already has permission '" + permission.getName() + "'");
        }

        role.getPermissions().add(permission);
        return toResponse(roleRepository.save(role));
    }

    @Transactional
    public RoleResponse removePermission(UUID roleId, UUID permissionId) {
        Role role = findById(roleId);
        Permission permission = findPermissionById(permissionId);

        boolean removed = role.getPermissions().removeIf(p -> p.getId().equals(permissionId));
        if (!removed) {
            throw AppException.notFound(
                    "Role '" + role.getName() + "' does not have permission '" + permission.getName() + "'");
        }

        return toResponse(roleRepository.save(role));
    }

    // ── Delete ───────────────────────────────────────────────────────────────

    @Transactional
    public void delete(UUID id) {
        Role role = findById(id);

        if (userRepository.existsUserWithRole(id)) {
            throw AppException.conflict(
                    "Role '" + role.getName() + "' is still assigned to one or more users" +
                    " — reassign all users before deleting this role");
        }

        roleRepository.delete(role);
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private Role findById(UUID id) {
        return roleRepository.findById(id)
                .orElseThrow(() -> AppException.notFound("Role not found: " + id));
    }

    private Permission findPermissionById(UUID id) {
        return permissionRepository.findById(id)
                .orElseThrow(() -> AppException.notFound("Permission not found: " + id));
    }

    private Set<Permission> resolvePermissions(List<UUID> ids) {
        if (ids == null || ids.isEmpty()) {
            return new HashSet<>();
        }
        Set<Permission> found = new HashSet<>(permissionRepository.findAllById(ids));
        if (found.size() != ids.size()) {
            Set<UUID> foundIds = found.stream().map(Permission::getId).collect(Collectors.toSet());
            List<UUID> missing = ids.stream().filter(id -> !foundIds.contains(id)).toList();
            throw AppException.notFound("Permissions not found: " + missing);
        }
        return found;
    }

    public RoleResponse toResponse(Role role) {
        return new RoleResponse(
                role.getId(),
                role.getName(),
                role.getDescription(),
                role.getPermissions().stream()
                        .map(permissionService::toResponse)
                        .collect(Collectors.toUnmodifiableSet()),
                role.getCreatedAt());
    }
}
