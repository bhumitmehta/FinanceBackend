package org.example.financebackend.service;

import lombok.RequiredArgsConstructor;
import org.example.financebackend.dto.request.CreatePermissionRequest;
import org.example.financebackend.dto.response.PermissionResponse;
import org.example.financebackend.exception.AppException;
import org.example.financebackend.model.Permission;
import org.example.financebackend.repository.PermissionRepository;
import org.example.financebackend.repository.RoleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PermissionService {

    private final PermissionRepository permissionRepository;
    private final RoleRepository       roleRepository;

    // ── Read ─────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<PermissionResponse> getAllPermissions() {
        return permissionRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public PermissionResponse getById(UUID id) {
        return toResponse(findById(id));
    }

    // ── Create ───────────────────────────────────────────────────────────────

    @Transactional
    public PermissionResponse create(CreatePermissionRequest request) {
        // Validate name format consistency with resource:action fields
        String expectedName = request.resource().trim().toLowerCase() + ":" + request.action().trim().toLowerCase();
        if (!request.name().equals(expectedName)) {
            throw AppException.badRequest(
                    "name must match resource:action — expected '" + expectedName + "'");
        }

        if (permissionRepository.findByName(request.name()).isPresent()) {
            throw AppException.conflict("Permission '" + request.name() + "' already exists");
        }

        Permission permission = Permission.builder()
                .name(request.name())
                .resource(request.resource().trim().toLowerCase())
                .action(request.action().trim().toLowerCase())
                .description(request.description())
                .build();

        return toResponse(permissionRepository.save(permission));
    }

    // ── Delete ───────────────────────────────────────────────────────────────

    @Transactional
    public void delete(UUID id) {
        Permission permission = findById(id);

        if (roleRepository.existsByPermissionsId(id)) {
            throw AppException.conflict(
                    "Permission '" + permission.getName() + "' is still assigned to one or more roles" +
                    " — remove it from all roles before deleting");
        }

        permissionRepository.delete(permission);
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private Permission findById(UUID id) {
        return permissionRepository.findById(id)
                .orElseThrow(() -> AppException.notFound("Permission not found: " + id));
    }

    public PermissionResponse toResponse(Permission p) {
        return new PermissionResponse(
                p.getId(),
                p.getName(),
                p.getResource(),
                p.getAction(),
                p.getDescription(),
                p.getCreatedAt());
    }
}
