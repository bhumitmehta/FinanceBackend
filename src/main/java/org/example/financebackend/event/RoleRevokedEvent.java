package org.example.financebackend.event;

import org.example.financebackend.model.Role;
import org.example.financebackend.model.User;

/**
 * Published when a {@link Role} is removed from a user's role set.
 * Handled by {@link AuditListener} to persist a {@code user_role_history} row.
 */
public class RoleRevokedEvent {

    private final User user;
    private final Role role;
    private final User revokedBy;
    private final String reason;

    public RoleRevokedEvent(User user, Role role, User revokedBy, String reason) {
        this.user      = user;
        this.role      = role;
        this.revokedBy = revokedBy;
        this.reason    = reason;
    }

    public User   getUser()      { return user;      }
    public Role   getRole()      { return role;      }
    public User   getRevokedBy() { return revokedBy; }
    public String getReason()    { return reason;    }
}
