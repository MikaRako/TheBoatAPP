package com.boatmanagement.dto;

import lombok.Builder;
import lombok.Getter;
import org.springframework.lang.Nullable;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Read-only DTOs for the audit log API.
 * Audit records are never mutated via the API — no Request DTO exists.
 *
 * @Nullable marks fields that are legitimately absent for certain audit
 * actions (e.g. resourceId is null for BOAT_LIST). This annotation is
 * understood by JDT's null-flow analyser and eliminates unchecked-conversion
 * warnings when building these DTOs from @Nullable entity fields.
 */
public class AuditLogDto {

    @Getter
    @Builder
    public static class Response {
        @Nullable private Long id;
        @Nullable private String username;
        @Nullable private String action;
        @Nullable private String resourceType;
        @Nullable private Long resourceId;
        @Nullable private String outcome;
        @Nullable private String errorMessage;
        @Nullable private Map<String, Object> metadata;
        @Nullable private String ipAddress;
        @Nullable private String userAgent;
        @Nullable private Instant occurredAt;
    }

    @Getter
    @Builder
    public static class PageResponse {
        @Nullable private List<Response> content;
        private int page;
        private int size;
        private long totalElements;
        private int totalPages;
        private boolean last;
    }
}
