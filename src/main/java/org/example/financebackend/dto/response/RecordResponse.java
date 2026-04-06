package org.example.financebackend.dto.response;

import org.example.financebackend.model.RecordType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record RecordResponse(
        UUID id,
        BigDecimal amount,
        RecordType type,
        String category,
        LocalDate date,
        String notes
) {}
