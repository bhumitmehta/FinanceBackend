package org.example.financebackend.dto.response;

import org.example.financebackend.model.RecordType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * One Envers revision of a FinancialRecord.
 *
 * @param revision   Envers global revision number (monotonically increasing)
 * @param timestamp  UTC date-time when the change was committed
 * @param changeType ADD | MOD | DEL (Envers RevisionType)
 * @param id         Record UUID (constant across all revisions)
 * @param amount     Amount at this revision
 * @param type       RecordType at this revision
 * @param category   Category at this revision
 * @param date       Transaction date at this revision
 * @param notes      Notes at this revision (may be null)
 * @param deletedAt  Soft-delete timestamp at this revision (null if not deleted)
 */
public record RecordHistoryResponse(
        int            revision,
        LocalDateTime  timestamp,
        String         changeType,
        UUID           id,
        BigDecimal     amount,
        RecordType     type,
        String         category,
        LocalDate      date,
        String         notes,
        LocalDateTime  deletedAt
) {}
