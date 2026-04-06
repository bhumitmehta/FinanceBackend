package org.example.financebackend.repository;

import org.example.financebackend.model.Role;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RoleRepository extends JpaRepository<Role, UUID> {

    Optional<Role> findByName(String name);

    List<Role> findByNameIn(Collection<String> names);

    /** Returns true if at least one role has this permission assigned. */
    boolean existsByPermissionsId(UUID permissionId);
}
