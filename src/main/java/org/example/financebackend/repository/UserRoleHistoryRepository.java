package org.example.financebackend.repository;

import org.example.financebackend.model.UserRoleHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface UserRoleHistoryRepository extends JpaRepository<UserRoleHistory, UUID> {

    List<UserRoleHistory> findByUserIdOrderByChangedAtDesc(UUID userId);

    List<UserRoleHistory> findByChangedByIdOrderByChangedAtDesc(UUID changedById);
}
