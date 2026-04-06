package org.example.financebackend.repository;

import org.example.financebackend.model.FinancialRecord;
import org.example.financebackend.model.RecordType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RecordRepository extends JpaRepository<FinancialRecord, UUID>,
        JpaSpecificationExecutor<FinancialRecord> {

    // Total amount for a user by record type (soft-delete aware)
    @Query("SELECT SUM(r.amount) FROM FinancialRecord r " +
           "WHERE r.user.id = :userId AND r.type = :type AND r.deletedAt IS NULL")
    Optional<BigDecimal> sumByUserAndType(@Param("userId") UUID userId,
                                          @Param("type") RecordType type);

    // Sum per category for a user (soft-delete aware) — returns [category, total]
    @Query("SELECT r.category, SUM(r.amount) FROM FinancialRecord r " +
           "WHERE r.user.id = :userId AND r.deletedAt IS NULL " +
           "GROUP BY r.category ORDER BY SUM(r.amount) DESC")
    List<Object[]> categorySums(@Param("userId") UUID userId);

    // Monthly totals per type using a native PostgreSQL DATE_TRUNC query
    // Returns rows of [month (Timestamp), type (String), total (BigDecimal)]
    @Query(value = "SELECT DATE_TRUNC('month', date) AS month, type, SUM(amount) AS total " +
                   "FROM financial_records " +
                   "WHERE user_id = :userId AND deleted_at IS NULL " +
                   "GROUP BY month, type ORDER BY month",
           nativeQuery = true)
    List<Object[]> monthlyTrends(@Param("userId") UUID userId);

    // ── Dashboard queries ────────────────────────────────────────────────────

    // Returns [type, total] grouped by record type within an inclusive date range
    @Query("SELECT r.type, SUM(r.amount) FROM FinancialRecord r " +
           "WHERE r.user.id = :userId AND r.deletedAt IS NULL " +
           "AND r.date >= :startDate AND r.date <= :endDate " +
           "GROUP BY r.type")
    List<Object[]> getTotals(@Param("userId") UUID userId,
                             @Param("startDate") LocalDate startDate,
                             @Param("endDate") LocalDate endDate);

    // Returns [category, total] grouped by category within an inclusive date range
    @Query("SELECT r.category, SUM(r.amount) FROM FinancialRecord r " +
           "WHERE r.user.id = :userId AND r.deletedAt IS NULL " +
           "AND r.date >= :startDate AND r.date <= :endDate " +
           "GROUP BY r.category ORDER BY SUM(r.amount) DESC")
    List<Object[]> getCategoryTotals(@Param("userId") UUID userId,
                                     @Param("startDate") LocalDate startDate,
                                     @Param("endDate") LocalDate endDate);

    // Returns [year, month, type, total] for monthly trend aggregation within an inclusive date range
    @Query("SELECT YEAR(r.date), MONTH(r.date), r.type, SUM(r.amount) FROM FinancialRecord r " +
           "WHERE r.user.id = :userId AND r.deletedAt IS NULL " +
           "AND r.date >= :startDate AND r.date <= :endDate " +
           "GROUP BY YEAR(r.date), MONTH(r.date), r.type " +
           "ORDER BY YEAR(r.date), MONTH(r.date)")
    List<Object[]> getMonthlyTrends(@Param("userId") UUID userId,
                                    @Param("startDate") LocalDate startDate,
                                    @Param("endDate") LocalDate endDate);

    // Returns the most recent N non-deleted records for a user (sorted by date desc)
    @Query("SELECT r FROM FinancialRecord r " +
           "WHERE r.user.id = :userId AND r.deletedAt IS NULL " +
           "ORDER BY r.date DESC, r.id DESC")
    List<FinancialRecord> findRecentActive(@Param("userId") UUID userId, Pageable pageable);
}
