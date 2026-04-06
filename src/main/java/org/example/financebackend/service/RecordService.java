package org.example.financebackend.service;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.example.financebackend.dto.request.CreateRecordRequest;
import org.example.financebackend.dto.request.UpdateRecordRequest;
import org.example.financebackend.dto.response.RecordHistoryResponse;
import org.example.financebackend.dto.response.RecordResponse;
import org.example.financebackend.event.RecordCreatedEvent;
import org.example.financebackend.event.RecordDeletedEvent;
import org.example.financebackend.exception.AppException;
import org.example.financebackend.mapper.RecordMapper;
import org.example.financebackend.model.FinancialRecord;
import org.example.financebackend.model.RecordType;
import org.example.financebackend.model.User;
import org.example.financebackend.repository.RecordRepository;
import org.example.financebackend.repository.UserRepository;
import org.example.financebackend.repository.spec.RecordSpecification;
import org.hibernate.envers.AuditReader;
import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.envers.RevisionType;
import org.hibernate.envers.query.AuditEntity;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RecordService {

    private final RecordRepository           recordRepository;
    private final UserRepository             userRepository;
    private final RecordMapper               recordMapper;
    private final ApplicationEventPublisher  eventPublisher;
    private final EntityManager              entityManager;

    // ── Create ─────────────────────────────────────────────────────
    @Transactional
    public RecordResponse create(CreateRecordRequest request) {
        User currentUser = currentUser();
        FinancialRecord record = recordMapper.toEntity(request);
        record.setUser(currentUser);
        FinancialRecord saved = recordRepository.save(record);
        eventPublisher.publishEvent(new RecordCreatedEvent(this, saved.getId(), currentUser.getId()));
        return recordMapper.toResponse(saved);
    }

    // ── Read all (with dynamic Specification chain) ────────────────────────
    @Transactional(readOnly = true)
    public Page<RecordResponse> getAll(RecordType type,
                                       String category,
                                       LocalDate startDate,
                                       LocalDate endDate,
                                       Pageable pageable) {
        Specification<FinancialRecord> spec = Specification.where(RecordSpecification.excludeDeleted());

        if (type != null) {
            spec = spec.and(RecordSpecification.byType(type));
        }
        if (category != null && !category.isBlank()) {
            spec = spec.and(RecordSpecification.byCategory(category));
        }
        if (startDate != null && endDate != null) {
            spec = spec.and(RecordSpecification.byDateRange(startDate, endDate));
        }

        return recordRepository.findAll(spec, pageable).map(recordMapper::toResponse);
    }

    // ── Read one ─────────────────────────────────────────────────────────
    @Transactional(readOnly = true)
    public RecordResponse getById(UUID id) {
        return recordMapper.toResponse(findActive(id));
    }

    // ── Update ───────────────────────────────────────────────────────────
    @Transactional
    public RecordResponse update(UUID id, UpdateRecordRequest request) {
        FinancialRecord record = findActive(id);
        recordMapper.updateEntity(request, record);
        return recordMapper.toResponse(recordRepository.save(record));
    }

    // ── Soft-delete ──────────────────────────────────────────────────────
    @Transactional
    public void softDelete(UUID id) {
        FinancialRecord record = findActive(id);
        record.setDeletedAt(LocalDateTime.now());
        recordRepository.save(record);
        eventPublisher.publishEvent(new RecordDeletedEvent(this, id, record.getUser().getId()));
    }

    // ── Audit history ────────────────────────────────────────────────────
    @Transactional(readOnly = true)
    public List<RecordHistoryResponse> getRecordHistory(UUID id) {
        // Verify the record exists at all (deleted or active)
        if (!recordRepository.existsById(id)) {
            throw AppException.notFound("Record not found: " + id);
        }

        AuditReader reader = AuditReaderFactory.get(entityManager);

        @SuppressWarnings("unchecked")
        List<Object[]> revisions = reader.createQuery()
                .forRevisionsOfEntity(FinancialRecord.class, false, true)
                .add(AuditEntity.id().eq(id))
                .getResultList();

        return revisions.stream()
                .map(row -> {
                    FinancialRecord snapshot  = (FinancialRecord) row[0];
                    org.hibernate.envers.DefaultRevisionEntity rev = (org.hibernate.envers.DefaultRevisionEntity) row[1];
                    RevisionType revType      = (RevisionType) row[2];

                    LocalDateTime timestamp = LocalDateTime.ofInstant(
                            Instant.ofEpochMilli(rev.getTimestamp()), ZoneOffset.UTC);

                    return new RecordHistoryResponse(
                            rev.getId(),
                            timestamp,
                            revType.name(),
                            snapshot.getId(),
                            snapshot.getAmount(),
                            snapshot.getType(),
                            snapshot.getCategory(),
                            snapshot.getDate(),
                            snapshot.getNotes(),
                            snapshot.getDeletedAt()
                    );
                })
                .sorted(Comparator.comparingInt(RecordHistoryResponse::revision).reversed())
                .toList();
    }

    // ── Helpers ────────────────────────────────────────────────────────────
    private FinancialRecord findActive(UUID id) {
        FinancialRecord record = recordRepository.findById(id)
                .orElseThrow(() -> AppException.notFound("Record not found: " + id));
        if (record.getDeletedAt() != null) {
            throw AppException.notFound("Record not found: " + id);
        }
        return record;
    }

    private User currentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> AppException.notFound("Authenticated user not found"));
    }
}
