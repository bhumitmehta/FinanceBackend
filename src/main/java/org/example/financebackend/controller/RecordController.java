package org.example.financebackend.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.financebackend.dto.request.CreateRecordRequest;
import org.example.financebackend.dto.request.UpdateRecordRequest;
import org.example.financebackend.dto.response.RecordHistoryResponse;
import org.example.financebackend.dto.response.RecordResponse;
import org.example.financebackend.model.RecordType;
import org.example.financebackend.service.RecordService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Tag(name = "Financial Records", description = "CRUD operations for financial records. Mutations require ADMIN role.")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/records")
@RequiredArgsConstructor
public class RecordController {

    private final RecordService recordService;

    // ── POST /api/records — write permission ────────────────────────────────────
    @Operation(summary = "Create record", description = "Requires records:write — creates a new financial record")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('PERM_records:write')")
    public RecordResponse create(@RequestBody @Valid CreateRecordRequest request) {
        return recordService.create(request);
    }

    // ── GET /api/records — all authenticated roles ────────────────────────────
    @Operation(summary = "List records", description = "Paginated + filterable list of active records. Supports type, category, startDate, endDate, page, size.")
    @GetMapping
    public Page<RecordResponse> getAll(
            @RequestParam(required = false) RecordType type,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @PageableDefault(size = 20, sort = "date") Pageable pageable) {
        return recordService.getAll(type, category, startDate, endDate, pageable);
    }

    // ── GET /api/records/{id} — all authenticated roles ───────────────────────
    @Operation(summary = "Get record by ID")
    @GetMapping("/{id}")
    public RecordResponse getById(@PathVariable UUID id) {
        return recordService.getById(id);
    }

    // ── PUT /api/records/{id} — write permission ───────────────────────────────
    @Operation(summary = "Update record", description = "Requires records:write — replaces all fields (null fields are cleared)")
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('PERM_records:write')")
    public RecordResponse update(@PathVariable UUID id,
                                 @RequestBody @Valid UpdateRecordRequest request) {
        return recordService.update(id, request);
    }

    // ── DELETE /api/records/{id} — delete permission ────────────────────────────
    @Operation(summary = "Delete record", description = "Requires records:delete — soft delete (sets deletedAt, record excluded from all queries)")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('PERM_records:delete')")
    public void delete(@PathVariable UUID id) {
        recordService.softDelete(id);
    }

    // ── GET /api/records/{id}/history — history permission ──────────────────────
    @Operation(summary = "Record audit history",
               description = "Requires records:history — returns all Envers revisions for a record, sorted by revision descending")
    @GetMapping("/{id}/history")
    @PreAuthorize("hasAuthority('PERM_records:history')")
    public List<RecordHistoryResponse> getHistory(@PathVariable UUID id) {
        return recordService.getRecordHistory(id);
    }
}
