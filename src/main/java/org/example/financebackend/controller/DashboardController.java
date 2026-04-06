package org.example.financebackend.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.example.financebackend.dto.response.CategoryTotalResponse;
import org.example.financebackend.dto.response.DashboardSummaryResponse;
import org.example.financebackend.dto.response.RecordResponse;
import org.example.financebackend.dto.response.TrendResponse;
import org.example.financebackend.service.DashboardService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@Tag(name = "Dashboard", description = "Aggregated analytics — all authenticated roles")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @Operation(summary = "Full summary",
               description = "Returns totals, category breakdown, monthly trends, and recent activity — all computed in parallel.")
    @GetMapping("/summary")
    @PreAuthorize("hasAuthority('PERM_dashboard:view')")
    public DashboardSummaryResponse getSummary(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return dashboardService.getFullSummary(startDate, endDate);
    }

    @Operation(summary = "Category totals",
               description = "Returns [{category, total}] sorted by total descending for the given date range.")
    @GetMapping("/by-category")
    @PreAuthorize("hasAuthority('PERM_dashboard:view')")
    public List<CategoryTotalResponse> getByCategory(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return dashboardService.getByCategory(startDate, endDate);
    }

    @Operation(summary = "Monthly trends",
               description = "Returns [{period, income, expense}] grouped by calendar month (YYYY-MM).")
    @GetMapping("/trends")
    @PreAuthorize("hasAuthority('PERM_dashboard:view')")
    public List<TrendResponse> getTrends(
            @RequestParam(defaultValue = "monthly") String period,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return dashboardService.getMonthlyTrends(startDate, endDate);
    }

    @Operation(summary = "Recent activity",
               description = "Returns the latest N non-deleted records ordered by date descending.")
    @GetMapping("/recent")
    @PreAuthorize("hasAuthority('PERM_dashboard:view')")
    public List<RecordResponse> getRecent(
            @RequestParam(defaultValue = "10") int limit) {
        return dashboardService.getRecent(limit);
    }
}
