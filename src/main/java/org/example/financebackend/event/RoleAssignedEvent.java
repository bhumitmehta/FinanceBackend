package org.example.financebackend.event;

import org.example.financebackend.model.Role;
import org.example.financebackend.model.User;

/**
 * Published when a {@link Role} is added to a user's role set.
 * Handled by {@link AuditListener} to persist a {@code user_role_history} row.
 */
public class RoleAssignedEvent {

    private final User user;
    private final Role role;
    private final User assignedBy;
    private final String reason;

    public RoleAssignedEvent(User user, Role role, User assignedBy, String reason) {
        this.user       = user;
        this.role       = role;
        this.assignedBy = assignedBy;
        this.reason     = reason;
    }

    public User   getUser()       { return user;       }
    public Role   getRole()       { return role;       }
    public User   getAssignedBy() { return assignedBy; }
    public String getReason()     { return reason;     }
}
