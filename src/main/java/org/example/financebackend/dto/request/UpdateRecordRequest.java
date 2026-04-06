package org.example.financebackend.dto.request;

import jakarta.validation.constraints.Positive;
import org.example.financebackend.model.RecordType;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * All fields are optional — only non-null fields are applied during update.
 */
public record UpdateRecordRequest(

        @Positive(message = "Amount must be positive")
        BigDecimal amount,

        RecordType type,

        String category,

        LocalDate date,

        String notes
) {}
