package org.example.financebackend.repository.spec;

import org.example.financebackend.model.FinancialRecord;
import org.example.financebackend.model.RecordType;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;

public final class RecordSpecification {

    private RecordSpecification() {}

    public static Specification<FinancialRecord> byType(RecordType type) {
        return (root, query, cb) -> cb.equal(root.get("type"), type);
    }

    public static Specification<FinancialRecord> byCategory(String category) {
        return (root, query, cb) -> cb.equal(root.get("category"), category);
    }

    public static Specification<FinancialRecord> byDateRange(LocalDate from, LocalDate to) {
        return (root, query, cb) -> cb.between(root.get("date"), from, to);
    }

    public static Specification<FinancialRecord> excludeDeleted() {
        return (root, query, cb) -> cb.isNull(root.get("deletedAt"));
    }
}
