package org.example.financebackend.service;

import lombok.RequiredArgsConstructor;
import org.example.financebackend.dto.response.CategoryTotalResponse;
import org.example.financebackend.dto.response.DashboardSummaryResponse;
import org.example.financebackend.dto.response.RecordResponse;
import org.example.financebackend.dto.response.TrendResponse;
import org.example.financebackend.exception.AppException;
import org.example.financebackend.mapper.RecordMapper;
import org.example.financebackend.model.RecordType;
import org.example.financebackend.repository.RecordRepository;
import org.example.financebackend.repository.UserRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DashboardService {

    /** Sentinel dates so queries always receive non-null bounds (avoids PostgreSQL type-inference error). */
    private static final LocalDate DATE_MIN = LocalDate.of(1900, 1, 1);
    private static final LocalDate DATE_MAX = LocalDate.of(9999, 12, 31);

    private final RecordRepository recordRepository;
    private final RecordMapper     recordMapper;
    private final UserRepository   userRepository;

    // Full summary - parallel fetch via CompletableFuture.allOf()
    public DashboardSummaryResponse getFullSummary(LocalDate startDate, LocalDate endDate) {
        UUID userId = currentUserId();
        LocalDate from = startDate != null ? startDate : DATE_MIN;
        LocalDate to   = endDate   != null ? endDate   : DATE_MAX;

        CompletableFuture<List<Object[]>> totalsFuture =
                CompletableFuture.supplyAsync(() -> recordRepository.getTotals(userId, from, to));
        CompletableFuture<List<Object[]>> categoryFuture =
                CompletableFuture.supplyAsync(() -> recordRepository.getCategoryTotals(userId, from, to));
        CompletableFuture<List<Object[]>> trendsFuture =
                CompletableFuture.supplyAsync(() -> recordRepository.getMonthlyTrends(userId, from, to));
        CompletableFuture<List<RecordResponse>> recentFuture =
                CompletableFuture.supplyAsync(() ->
                        recordRepository.findRecentActive(userId, PageRequest.of(0, 10))
                                .stream().map(recordMapper::toResponse).collect(Collectors.toList()));

        CompletableFuture.allOf(totalsFuture, categoryFuture, trendsFuture, recentFuture).join();

        Map<RecordType, BigDecimal> totals   = parseTotals(totalsFuture.join());
        BigDecimal income   = totals.getOrDefault(RecordType.INCOME,   BigDecimal.ZERO);
        BigDecimal expenses = totals.getOrDefault(RecordType.EXPENSE,  BigDecimal.ZERO);

        return new DashboardSummaryResponse(
                income, expenses, income.subtract(expenses),
                parseCategories(categoryFuture.join()),
                parseTrends(trendsFuture.join()),
                recentFuture.join()
        );
    }

    @Transactional(readOnly = true)
    public List<CategoryTotalResponse> getByCategory(LocalDate startDate, LocalDate endDate) {
        return parseCategories(recordRepository.getCategoryTotals(
                currentUserId(),
                startDate != null ? startDate : DATE_MIN,
                endDate   != null ? endDate   : DATE_MAX));
    }

    @Transactional(readOnly = true)
    public List<TrendResponse> getMonthlyTrends(LocalDate startDate, LocalDate endDate) {
        return parseTrends(recordRepository.getMonthlyTrends(
                currentUserId(),
                startDate != null ? startDate : DATE_MIN,
                endDate   != null ? endDate   : DATE_MAX));
    }

    @Transactional(readOnly = true)
    public List<RecordResponse> getRecent(int limit) {
        return recordRepository.findRecentActive(currentUserId(), PageRequest.of(0, limit))
                .stream().map(recordMapper::toResponse).collect(Collectors.toList());
    }

    private Map<RecordType, BigDecimal> parseTotals(List<Object[]> rows) {
        Map<RecordType, BigDecimal> map = new EnumMap<>(RecordType.class);
        for (Object[] row : rows) { map.put((RecordType) row[0], (BigDecimal) row[1]); }
        return map;
    }

    private List<CategoryTotalResponse> parseCategories(List<Object[]> rows) {
        return rows.stream()
                .map(row -> new CategoryTotalResponse((String) row[0], (BigDecimal) row[1]))
                .collect(Collectors.toList());
    }

    private List<TrendResponse> parseTrends(List<Object[]> rows) {
        Map<String, BigDecimal[]> periodMap = new LinkedHashMap<>();
        for (Object[] row : rows) {
            int year  = ((Number) row[0]).intValue();
            int month = ((Number) row[1]).intValue();
            RecordType type  = (RecordType) row[2];
            BigDecimal total = (BigDecimal)  row[3];
            String period = String.format("%04d-%02d", year, month);
            periodMap.computeIfAbsent(period, k -> new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO});
            if (type == RecordType.INCOME)  { periodMap.get(period)[0] = total; }
            else if (type == RecordType.EXPENSE) { periodMap.get(period)[1] = total; }
        }
        return periodMap.entrySet().stream()
                .map(e -> new TrendResponse(e.getKey(), e.getValue()[0], e.getValue()[1]))
                .collect(Collectors.toList());
    }

    private UUID currentUserId() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> AppException.notFound("Authenticated user not found"))
                .getId();
    }
}