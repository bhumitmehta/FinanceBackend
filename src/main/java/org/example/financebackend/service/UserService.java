package org.example.financebackend.service;

import lombok.RequiredArgsConstructor;
import org.example.financebackend.dto.request.RegisterRequest;
import org.example.financebackend.dto.request.ToggleStatusRequest;
import org.example.financebackend.dto.response.PermissionResponse;
import org.example.financebackend.dto.response.UserResponse;
import org.example.financebackend.dto.response.UserRoleHistoryResponse;
import org.example.financebackend.event.RoleAssignedEvent;
import org.example.financebackend.event.RoleRevokedEvent;
import org.example.financebackend.exception.AppException;
import org.example.financebackend.mapper.UserMapper;
import org.example.financebackend.model.*;
import org.example.financebackend.repository.RoleRepository;
import org.example.financebackend.repository.UserRepository;
import org.example.financebackend.repository.UserRoleHistoryRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository             userRepository;
    private final UserRoleHistoryRepository  roleHistoryRepository;
    private final RoleRepository             roleRepository;
    private final PermissionService          permissionService;
    private final PasswordEncoder            passwordEncoder;
    private final UserMapper                 userMapper;
    private final ApplicationEventPublisher  eventPublisher;

    // ── Read ─────────────────────────────────────────────────────────────────
    @Transactional(readOnly = true)
    public List<UserResponse> getAllUsers() {
        return userRepository.findAll().stream()
                .map(userMapper::toResponse)
                .toList();
    }

    // ── Create ───────────────────────────────────────────────────────────────
    @Transactional
    public UserResponse createUser(RegisterRequest request) {
        if (userRepository.findByEmail(request.email()).isPresent()) {
            throw new IllegalArgumentException("Email already registered");
        }
        User user = User.builder()
                .name(request.name())
                .email(request.email())
                .passwordHash(passwordEncoder.encode(request.password()))
                .status(UserStatus.ACTIVE)
                .build();
        return userMapper.toResponse(userRepository.save(user));
    }

    // ── Assign role ──────────────────────────────────────────────────────────

    /**
     * Appends {@code roleId} to the user's role set.
     * Idempotent guard: throws 409 if the user already holds this role.
     * Publishes {@link RoleAssignedEvent} → {@link org.example.financebackend.event.AuditListener} persists history row.
     */
    @Transactional
    public UserResponse assignRole(UUID userId, UUID roleId) {
        return assignRole(userId, roleId, null);
    }

    @Transactional
    public UserResponse assignRole(UUID userId, UUID roleId, String reason) {
        User user = findByIdWithRoles(userId);
        Role role = findRoleById(roleId);
        User admin = currentUser();

        boolean alreadyHasRole = user.getUserRoles().stream()
                .anyMatch(ur -> ur.getRole().getId().equals(roleId));
        if (alreadyHasRole) {
            throw AppException.conflict("User already has role '" + role.getName() + "'");
        }

        UserRoleId pk = new UserRoleId(user.getId(), roleId);
        UserRole userRole = UserRole.builder()
                .id(pk)
                .user(user)
                .role(role)
                .assignedBy(admin)
                .assignedAt(LocalDateTime.now())
                .build();
        user.getUserRoles().add(userRole);
        User saved = userRepository.save(user);

        eventPublisher.publishEvent(new RoleAssignedEvent(saved, role, admin, reason));
        return userMapper.toResponse(saved);
    }

    // ── Revoke role ───────────────────────────────────────────────────────────

    /**
     * Removes {@code roleId} from the user's role set.
     * Guard: throws 400 if this is the user's last role.
     * Publishes {@link RoleRevokedEvent} → {@link org.example.financebackend.event.AuditListener} persists history row.
     */
    @Transactional
    public UserResponse revokeRole(UUID userId, UUID roleId) {
        return revokeRole(userId, roleId, null);
    }

    @Transactional
    public UserResponse revokeRole(UUID userId, UUID roleId, String reason) {
        User user = findByIdWithRoles(userId);
        Role role = findRoleById(roleId);
        User admin = currentUser();

        if (user.getUserRoles().size() <= 1) {
            throw AppException.badRequest(
                    "Cannot revoke the user's last role — assign a different role first");
        }

        boolean removed = user.getUserRoles().removeIf(
                ur -> ur.getRole().getId().equals(roleId));
        if (!removed) {
            throw AppException.notFound(
                    "User does not have role '" + role.getName() + "'");
        }

        User saved = userRepository.save(user);
        eventPublisher.publishEvent(new RoleRevokedEvent(saved, role, admin, reason));
        return userMapper.toResponse(saved);
    }

    // ── Effective permissions ─────────────────────────────────────────────────

    /**
     * Flattens all roles a user holds into a deduplicated set of permission names.
     * This is the Composite pattern in action: User → roles → permissions.
     */
    @Transactional(readOnly = true)
    public List<PermissionResponse> getUserPermissions(UUID userId) {
        User user = findByIdWithRoles(userId);
        return user.getUserRoles().stream()
                .flatMap(ur -> ur.getRole().getPermissions().stream())
                .distinct()
                .map(permissionService::toResponse)
                .toList();
    }

    // ── Role history ─────────────────────────────────────────────────────────
    @Transactional(readOnly = true)
    public List<UserRoleHistoryResponse> getRoleHistory(UUID userId) {
        if (!userRepository.existsById(userId)) {
            throw AppException.notFound("User not found: " + userId);
        }
        return roleHistoryRepository.findByUserIdOrderByChangedAtDesc(userId).stream()
                .map(h -> new UserRoleHistoryResponse(
                        h.getId(),
                        h.getUser().getId(),
                        h.getUser().getName(),
                        h.getPreviousRole(),
                        h.getNewRole(),
                        h.getChangedBy().getId(),
                        h.getChangedBy().getName(),
                        h.getReason(),
                        h.getChangedAt()))
                .toList();
    }

    // ── Toggle status ─────────────────────────────────────────────────────────
    @Transactional
    public UserResponse toggleStatus(UUID userId, ToggleStatusRequest request) {
        User user = findById(userId);
        user.setStatus(request.status());
        return userMapper.toResponse(userRepository.save(user));
    }

    // ── Helpers ───────────────────────────────────────────────────────────────
    private User findById(UUID id) {
        return userRepository.findById(id)
                .orElseThrow(() -> AppException.notFound("User not found: " + id));
    }

    private User findByIdWithRoles(UUID id) {
        return userRepository.findWithRolesAndPermissionsById(id)
                .orElseThrow(() -> AppException.notFound("User not found: " + id));
    }

    private Role findRoleById(UUID id) {
        return roleRepository.findById(id)
                .orElseThrow(() -> AppException.notFound("Role not found: " + id));
    }

    private User currentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException(HttpStatus.INTERNAL_SERVER_ERROR, "Authenticated user not found"));
    }
}


