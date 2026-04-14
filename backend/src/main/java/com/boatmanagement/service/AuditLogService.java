package com.boatmanagement.service;

import com.boatmanagement.entity.AuditAction;
import com.boatmanagement.entity.AuditLog;
import com.boatmanagement.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.Nullable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Map;

/**
 * Persists audit log records asynchronously so that slow I/O never
 * adds latency to an API response.
 *
 * Key design decisions:
 *
 * 1. @Async("auditExecutor") — runs on a dedicated thread pool defined in
 *    AsyncConfig.  Isolation prevents audit work from exhausting the HTTP
 *    worker pool, and gives the thread pool a meaningful name in thread dumps.
 *
 * 2. Propagation.REQUIRES_NEW — the audit INSERT always runs in its own
 *    transaction, independent of any outer business transaction.  This means:
 *    - A FAILURE record is still committed even when the calling transaction
 *      rolls back (exactly the scenario we need most).
 *    - The audit INSERT never holds locks on business tables.
 *
 * 3. No checked exceptions — any persistence error is caught and logged at
 *    ERROR level.  We never let audit failures propagate back to callers;
 *    an audit glitch must not kill a legitimate user request.
 *
 * @Nullable on optional parameters tells JDT that null is an accepted value,
 * eliminating unchecked-conversion warnings at every call site.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;

    /**
     * Persists a SUCCESS audit entry.
     *
     * @param action       the audited operation
     * @param username     authenticated user
     * @param resourceType logical resource name (e.g. "BOAT"); null for non-resource actions
     * @param resourceId   pk of the affected record; null for list/search
     * @param metadata     action-specific context bag (stored as JSONB); null if none
     * @param ipAddress    client IP; null outside HTTP context
     * @param userAgent    client UA; null outside HTTP context
     */
    @Async("auditExecutor")
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logSuccess(AuditAction action,
                           String username,
                           @Nullable String resourceType,
                           @Nullable Long resourceId,
                           @Nullable Map<String, Object> metadata,
                           @Nullable String ipAddress,
                           @Nullable String userAgent) {
        persist(action, username, resourceType, resourceId,
                AuditLog.Outcome.SUCCESS, null, metadata, ipAddress, userAgent);
    }

    /**
     * Persists a FAILURE audit entry.
     *
     * @param errorMessage short description of what went wrong (sanitised by caller); null if unavailable
     */
    @Async("auditExecutor")
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logFailure(AuditAction action,
                           String username,
                           @Nullable String resourceType,
                           @Nullable Long resourceId,
                           @Nullable String errorMessage,
                           @Nullable Map<String, Object> metadata,
                           @Nullable String ipAddress,
                           @Nullable String userAgent) {
        persist(action, username, resourceType, resourceId,
                AuditLog.Outcome.FAILURE, errorMessage, metadata, ipAddress, userAgent);
    }

    private void persist(AuditAction action,
                         String username,
                         @Nullable String resourceType,
                         @Nullable Long resourceId,
                         AuditLog.Outcome outcome,
                         @Nullable String errorMessage,
                         @Nullable Map<String, Object> metadata,
                         @Nullable String ipAddress,
                         @Nullable String userAgent) {
        try {
            AuditLog entry = AuditLog.builder()
                    .action(action)
                    .username(username)
                    .resourceType(resourceType)
                    .resourceId(resourceId)
                    .outcome(outcome)
                    .errorMessage(errorMessage != null && errorMessage.length() > 1000
                            ? errorMessage.substring(0, 1000)
                            : errorMessage)
                    .metadata(metadata)
                    .ipAddress(ipAddress)
                    .userAgent(userAgent)
                    .occurredAt(Instant.now())
                    .build();

            auditLogRepository.save(entry);
            log.debug("Audit record persisted: action={}, user={}, outcome={}, resourceType={}, resourceId={}",
                    action, username, outcome, resourceType, resourceId);

        } catch (Exception e) {
            // Never propagate — a broken audit subsystem must not affect users.
            log.error("Failed to persist audit log entry: action={}, user={}, outcome={}",
                    action, username, outcome, e);
        }
    }
}
