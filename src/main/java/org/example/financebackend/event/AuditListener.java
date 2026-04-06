package org.example.financebackend.event;

import lombok.RequiredArgsConstructor;
import org.example.financebackend.model.UserRoleHistory;
import org.example.financebackend.repository.UserRoleHistoryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

@Component
@RequiredArgsConstructor
public class AuditListener {

    private static final Logger log = LoggerFactory.getLogger(AuditListener.class);

    private final UserRoleHistoryRepository roleHistoryRepository;

    @EventListener
    public void onRecordCreated(RecordCreatedEvent event) {
        log.info("[AUDIT] action=RECORD_CREATED recordId={} userId={} timestamp={}",
                event.getRecordId(), event.getUserId(), Instant.now());
    }

    @EventListener
    public void onRecordDeleted(RecordDeletedEvent event) {
        log.info("[AUDIT] action=RECORD_DELETED recordId={} userId={} timestamp={}",
                event.getRecordId(), event.getUserId(), Instant.now());
    }

    /** Legacy handler for the old single-role-change event (kept for backward compatibility). */
    @EventListener
    public void onRoleChanged(RoleChangedEvent event) {
        String prevName = event.getPreviousRole() != null ? event.getPreviousRole().name().toLowerCase() : null;
        String newName  = event.getNewRole()      != null ? event.getNewRole().name().toLowerCase()      : null;
        log.info("[AUDIT] action=ROLE_CHANGED userId={} {} -> {} changedBy={} reason={}",
                event.getUser().getId(), prevName, newName,
                event.getChangedBy().getId(), event.getReason());

        roleHistoryRepository.save(UserRoleHistory.builder()
                .user(event.getUser())
                .previousRole(prevName)
                .newRole(newName)
                .changedBy(event.getChangedBy())
                .reason(event.getReason())
                .changedAt(LocalDateTime.ofInstant(Instant.now(), ZoneOffset.UTC))
                .build());
    }

    @EventListener
    public void onRoleAssigned(RoleAssignedEvent event) {
        log.info("[AUDIT] action=ROLE_ASSIGNED userId={} role={} assignedBy={} reason={}",
                event.getUser().getId(), event.getRole().getName(),
                event.getAssignedBy().getId(), event.getReason());

        roleHistoryRepository.save(UserRoleHistory.builder()
                .user(event.getUser())
                .previousRole(null)
                .newRole(event.getRole().getName())
                .changedBy(event.getAssignedBy())
                .reason(event.getReason())
                .changedAt(LocalDateTime.ofInstant(Instant.now(), ZoneOffset.UTC))
                .build());
    }

    @EventListener
    public void onRoleRevoked(RoleRevokedEvent event) {
        log.info("[AUDIT] action=ROLE_REVOKED userId={} role={} revokedBy={} reason={}",
                event.getUser().getId(), event.getRole().getName(),
                event.getRevokedBy().getId(), event.getReason());

        roleHistoryRepository.save(UserRoleHistory.builder()
                .user(event.getUser())
                .previousRole(event.getRole().getName())
                .newRole(null)
                .changedBy(event.getRevokedBy())
                .reason(event.getReason())
                .changedAt(LocalDateTime.ofInstant(Instant.now(), ZoneOffset.UTC))
                .build());
    }
}
