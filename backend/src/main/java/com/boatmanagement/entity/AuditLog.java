package com.boatmanagement.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.lang.Nullable;

import java.time.Instant;
import java.util.Map;

/**
 * Immutable audit log record.
 *
 * Design decisions:
 * - No @Setter / no @Entity mutators: audit rows must never be updated after insert.
 * - JSONB column (metadata) stores flexible, action-specific context without
 *   requiring schema changes for every new event type.
 * - outcome distinguishes SUCCESS from FAILURE so security teams can run
 *   "failed action" queries with a simple WHERE clause.
 * - ip_address and user_agent support forensic investigation; they are nullable
 *   because background/async callers may not have an HTTP context.
 * - No @UpdateTimestamp — Hibernate must never touch this row again after insert.
 * - @Nullable on optional fields tells JDT (and Lombok's builder) that these
 *   fields are intentionally nullable, eliminating unchecked-conversion warnings.
 */
@Entity
@Table(
    name = "audit_log",
    indexes = {
        @Index(name = "idx_audit_log_username",    columnList = "username"),
        @Index(name = "idx_audit_log_action",      columnList = "action"),
        @Index(name = "idx_audit_log_resource",    columnList = "resource_type, resource_id"),
        @Index(name = "idx_audit_log_occurred_at", columnList = "occurred_at"),
        @Index(name = "idx_audit_log_outcome",     columnList = "outcome")
    }
)
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "audit_log_seq")
    @SequenceGenerator(name = "audit_log_seq", sequenceName = "audit_log_sequence", allocationSize = 50)
    @Nullable
    private Long id;

    /** Keycloak preferred_username or subject (UUID) if username is absent. */
    @Column(nullable = false, length = 255)
    private String username;

    /** The auditable action that occurred. */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 64)
    private AuditAction action;

    /** Resource type, e.g. "BOAT". Enables cross-resource queries. */
    @Nullable
    @Column(name = "resource_type", length = 64)
    private String resourceType;

    /** Primary key of the affected resource; null for list/search actions. */
    @Nullable
    @Column(name = "resource_id")
    private Long resourceId;

    /** SUCCESS or FAILURE. */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private Outcome outcome;

    /**
     * Human-readable error message when outcome = FAILURE.
     * Truncated to 1000 chars to prevent log injection and storage bloat.
     */
    @Nullable
    @Column(name = "error_message", length = 1000)
    private String errorMessage;

    /**
     * Flexible JSONB bag for action-specific context:
     *   - BOAT_CREATE  → {"name": "...", "type": "...", "status": "..."}
     *   - BOAT_UPDATE  → {"before": {...}, "after": {...}}
     *   - BOAT_LIST    → {"search": "...", "status": "...", "page": 0}
     * Storing as JSONB means Postgres can index and query inside it.
     */
    @Nullable
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> metadata;

    /** Client IP address; null for internal/async events. */
    @Nullable
    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    /** Client user-agent string (truncated). */
    @Nullable
    @Column(name = "user_agent", length = 512)
    private String userAgent;

    /** Wall-clock time of the event; set at build time, never updated. */
    @Column(name = "occurred_at", nullable = false, updatable = false)
    private Instant occurredAt;

    public enum Outcome {
        SUCCESS, FAILURE
    }
}
