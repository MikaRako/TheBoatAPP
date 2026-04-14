package com.boatmanagement.controller;

import com.boatmanagement.dto.AuditLogDto;
import com.boatmanagement.entity.AuditAction;
import com.boatmanagement.entity.AuditLog;
import com.boatmanagement.repository.AuditLogRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;

/**
 * Read-only REST API for audit logs. Restricted to ROLE_ADMIN.
 *
 * Security decisions:
 * - @PreAuthorize("hasRole('ADMIN')") — only admins can read audit logs.
 * Regular users cannot see who else did what, protecting privacy.
 * - No write endpoints exist — audit records are insert-only by design.
 * - IP addresses and user-agents are included in the response because
 * admins need them for forensic investigation; they are not surfaced
 * to regular users anywhere in the application.
 */
@RestController
@RequestMapping("/api/audit-logs")
@RequiredArgsConstructor
@Tag(name = "Audit Logs", description = "Admin-only audit trail API")
@SecurityRequirement(name = "bearerAuth")
public class AuditLogController {

    private final AuditLogRepository auditLogRepository;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Search audit logs", description = "Multi-criteria search over the audit trail. All filters are optional and combined with AND.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Results returned"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden — ROLE_ADMIN required")
    })
    public ResponseEntity<AuditLogDto.PageResponse> search(
            @Parameter(description = "Filter by username") @RequestParam(required = false) @Nullable String username,

            @Parameter(description = "Filter by action (e.g. BOAT_CREATE)") @RequestParam(required = false) @Nullable AuditAction action,

            @Parameter(description = "Filter by resource type (e.g. BOAT)") @RequestParam(required = false) @Nullable String resourceType,

            @Parameter(description = "Filter by outcome (SUCCESS or FAILURE)") @RequestParam(required = false) @Nullable AuditLog.Outcome outcome,

            @Parameter(description = "From timestamp (ISO-8601, inclusive)") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) @Nullable Instant from,

            @Parameter(description = "To timestamp (ISO-8601, inclusive)") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) @Nullable Instant to,

            @Parameter(description = "Page number (0-indexed)") @RequestParam(defaultValue = "0") int page,

            @Parameter(description = "Page size (max 100)") @RequestParam(defaultValue = "20") int size,

            @Parameter(description = "Sort field") @RequestParam(defaultValue = "occurredAt") String sortBy,

            @Parameter(description = "Sort direction (asc/desc)") @RequestParam(defaultValue = "desc") String sortDir) {

        // Cap page size to prevent accidental large result sets
        int cappedSize = Math.min(size, 100);
        Sort sort = sortDir.equalsIgnoreCase("asc")
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();

        Page<AuditLog> resultPage = auditLogRepository.search(
                username, action, resourceType, outcome, from, to,
                PageRequest.of(page, cappedSize, sort));

        AuditLogDto.PageResponse response = AuditLogDto.PageResponse.builder()
                .content(resultPage.getContent().stream().map(this::toDto).toList())
                .page(resultPage.getNumber())
                .size(resultPage.getSize())
                .totalElements(resultPage.getTotalElements())
                .totalPages(resultPage.getTotalPages())
                .last(resultPage.isLast())
                .build();

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get a single audit log entry by id")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Entry found"),
            @ApiResponse(responseCode = "404", description = "Not found"),
            @ApiResponse(responseCode = "403", description = "Forbidden — ROLE_ADMIN required")
    })
    public ResponseEntity<AuditLogDto.Response> getById(@PathVariable @NonNull Long id) {
        return auditLogRepository.findById(id)
                .map(this::toDto)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    private AuditLogDto.Response toDto(AuditLog log) {
        return AuditLogDto.Response.builder()
                .id(log.getId())
                .username(log.getUsername())
                .action(log.getAction().name())
                .resourceType(log.getResourceType())
                .resourceId(log.getResourceId())
                .outcome(log.getOutcome().name())
                .errorMessage(log.getErrorMessage())
                .metadata(log.getMetadata())
                .ipAddress(log.getIpAddress())
                .userAgent(log.getUserAgent())
                .occurredAt(log.getOccurredAt())
                .build();
    }
}
