package org.example.financebackend.dto.response;

import java.math.BigDecimal;

public record TrendResponse(
        String period,
        BigDecimal income,
        BigDecimal expense
) {}
