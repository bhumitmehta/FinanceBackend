package org.example.financebackend.repository;

import org.example.financebackend.model.User;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmail(String email);

    /**
     * Eagerly loads userRoles → role → permissions in a single JOIN query.
     * Use this in security-sensitive paths (e.g. loadUserByUsername) to avoid
     * N+1 queries when evaluating role/permission checks.
     */
    @EntityGraph(attributePaths = {"userRoles", "userRoles.role", "userRoles.role.permissions"})
    Optional<User> findWithRolesAndPermissionsByEmail(String email);

    /**
     * Eagerly loads userRoles → role → permissions for a user by ID in a single JOIN query.
     * Use this for permission-sensitive service operations to avoid N+1 queries.
     */
    @EntityGraph(attributePaths = {"userRoles", "userRoles.role", "userRoles.role.permissions"})
    Optional<User> findWithRolesAndPermissionsById(UUID id);

    /** Returns true if at least one user currently holds this role. */
    @Query("SELECT CASE WHEN COUNT(ur) > 0 THEN TRUE ELSE FALSE END FROM UserRole ur WHERE ur.role.id = :roleId")
    boolean existsUserWithRole(@Param("roleId") UUID roleId);
}
