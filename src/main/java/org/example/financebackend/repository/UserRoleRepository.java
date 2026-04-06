package org.example.financebackend.repository;

import org.example.financebackend.model.UserRole;
import org.example.financebackend.model.UserRoleId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRoleRepository extends JpaRepository<UserRole, UserRoleId> {
}
