package com.boatmanagement.repository;

import com.boatmanagement.entity.AuditAction;
import com.boatmanagement.entity.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

import java.time.Instant;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    // Redeclare with @NonNull so JDT's flow analyser knows save() never returns null.
    @Override
    @NonNull <S extends AuditLog> S save(@NonNull S entity);

    Page<AuditLog> findByUsername(@Nullable String username, Pageable pageable);

    Page<AuditLog> findByAction(@Nullable AuditAction action, Pageable pageable);

    Page<AuditLog> findByResourceTypeAndResourceId(
            @Nullable String resourceType,
            @Nullable Long resourceId,
            Pageable pageable);

    Page<AuditLog> findByOutcome(@Nullable AuditLog.Outcome outcome, Pageable pageable);

    /**
     * Flexible multi-criteria search used by the admin audit API.
     * All parameters are optional; null values are ignored via IS NULL guards.
     */
    @Query("""
            SELECT a FROM AuditLog a
            WHERE (:username     IS NULL OR a.username     = :username)
              AND (:action       IS NULL OR a.action       = :action)
              AND (:resourceType IS NULL OR a.resourceType = :resourceType)
              AND (:outcome      IS NULL OR a.outcome      = :outcome)
              AND (:from         IS NULL OR a.occurredAt  >= :from)
              AND (:to           IS NULL OR a.occurredAt  <= :to)
            ORDER BY a.occurredAt DESC
            """)
    Page<AuditLog> search(
            @Param("username")     @Nullable String username,
            @Param("action")       @Nullable AuditAction action,
            @Param("resourceType") @Nullable String resourceType,
            @Param("outcome")      @Nullable AuditLog.Outcome outcome,
            @Param("from")         @Nullable Instant from,
            @Param("to")           @Nullable Instant to,
            Pageable pageable);
}
