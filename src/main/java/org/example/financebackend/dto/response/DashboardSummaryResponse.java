package org.example.financebackend.dto.response;

import java.math.BigDecimal;
import java.util.List;

public record DashboardSummaryResponse(
        BigDecimal totalIncome,
        BigDecimal totalExpenses,
        BigDecimal netBalance,
        List<CategoryTotalResponse> byCategory,
        List<TrendResponse> trends,
        List<RecordResponse> recentActivity
) {}
