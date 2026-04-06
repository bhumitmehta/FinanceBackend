package org.example.financebackend.event;

import org.springframework.context.ApplicationEvent;

import java.util.UUID;

public class RecordDeletedEvent extends ApplicationEvent {

    private final UUID recordId;
    private final UUID userId;

    public RecordDeletedEvent(Object source, UUID recordId, UUID userId) {
        super(source);
        this.recordId = recordId;
        this.userId   = userId;
    }

    public UUID getRecordId() { return recordId; }
    public UUID getUserId()   { return userId;   }
}
