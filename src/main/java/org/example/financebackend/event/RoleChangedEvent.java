package org.example.financebackend.event;

import org.example.financebackend.model.RoleType;
import org.example.financebackend.model.User;

public class RoleChangedEvent {

    private final User user;
    private final RoleType previousRole;
    private final RoleType newRole;
    private final User changedBy;
    private final String reason;

    public RoleChangedEvent(User user, RoleType previousRole, RoleType newRole, User changedBy, String reason) {
        this.user         = user;
        this.previousRole = previousRole;
        this.newRole      = newRole;
        this.changedBy    = changedBy;
        this.reason       = reason;
    }

    public User   getUser()         { return user;         }
    public RoleType getPreviousRole() { return previousRole; }
    public RoleType getNewRole()      { return newRole;      }
    public User   getChangedBy()    { return changedBy;    }
    public String getReason()       { return reason;       }
}
