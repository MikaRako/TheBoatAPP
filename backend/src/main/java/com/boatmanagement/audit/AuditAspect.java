package com.boatmanagement.audit;

import com.boatmanagement.entity.AuditAction;
import com.boatmanagement.service.AuditLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

/**
 * AOP aspect that intercepts every @Auditable method and records a
 * SUCCESS or FAILURE audit entry after the method completes.
 *
 * Why @Around (not @AfterReturning + @AfterThrowing)?
 * Using a single @Around advice lets us capture the outcome in one place,
 * keep the success/failure paths symmetric, and avoid duplicating the
 * AuditContext reads (which must happen on the calling thread, before the
 * async handoff in AuditLogService).
 *
 * Thread-safety note:
 * All context extraction (username, IP, UA) happens HERE, synchronously,
 * on the HTTP worker thread where the SecurityContext is still populated.
 * AuditLogService then receives plain Strings — safe to hand off to the
 * async executor.
 */
@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class AuditAspect {

    private final AuditLogService auditLogService;

    @Around("@annotation(auditable)")
    public Object audit(ProceedingJoinPoint pjp, Auditable auditable) throws Throwable {

        // --- Capture HTTP / security context synchronously ---
        String username  = AuditContext.currentUsername();
        String ipAddress = AuditContext.currentIpAddress();
        String userAgent = AuditContext.currentUserAgent();

        AuditAction action       = auditable.action();
        String      resourceType = auditable.resourceType().isBlank() ? null : auditable.resourceType();
        int         idArgIndex   = auditable.resourceIdArgIndex();

        // Extract a resource id from the method arguments when annotated.
        Long resourceId = extractArgId(pjp.getArgs(), idArgIndex);

        Object result;
        try {
            result = pjp.proceed();
        } catch (Throwable ex) {
            // --- FAILURE path ---
            Map<String, Object> meta = buildFailureMetadata(pjp, ex);
            auditLogService.logFailure(
                    action, username, resourceType, resourceId,
                    sanitise(ex.getMessage()), meta, ipAddress, userAgent);
            throw ex;   // re-throw so Spring's exception handling is unaffected
        }

        // --- SUCCESS path ---
        // If no id was found in args, try to read it from the returned DTO.
        if (resourceId == null) {
            resourceId = extractReturnId(result);
        }
        Map<String, Object> meta = buildSuccessMetadata(pjp, result);
        auditLogService.logSuccess(
                action, username, resourceType, resourceId,
                meta, ipAddress, userAgent);

        return result;
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    /** Reads the id argument at the given index, or null if index is -1 / out of range. */
    @Nullable
    private Long extractArgId(@Nullable Object[] args, int index) {
        if (index < 0 || args == null || index >= args.length) return null;
        Object arg = args[index];
        if (arg instanceof Long l)    return l;
        if (arg instanceof Integer i) return i.longValue();
        return null;
    }

    /**
     * Reflectively calls getId() on the return value.
     * Handles BoatDto.Response and any other DTO that exposes getId().
     */
    @Nullable
    private Long extractReturnId(@Nullable Object result) {
        if (result == null) return null;
        try {
            Method getId = result.getClass().getMethod("getId");
            Object id = getId.invoke(result);
            if (id instanceof Long l)    return l;
            if (id instanceof Integer i) return i.longValue();
        } catch (NoSuchMethodException ignored) {
            // DTO has no getId — normal for void or Page responses
        } catch (InvocationTargetException | IllegalAccessException e) {
            log.warn("Unexpected error invoking getId() on {}: {}",
                    result.getClass().getSimpleName(), e.getMessage());
        }
        return null;
    }

    /** Builds a lightweight metadata map for successful invocations. */
    private Map<String, Object> buildSuccessMetadata(ProceedingJoinPoint pjp, @Nullable Object result) {
        MethodSignature sig = (MethodSignature) pjp.getSignature();
        Map<String, Object> meta = new HashMap<>();
        meta.put("method", sig.getDeclaringType().getSimpleName() + "." + sig.getName());
        // Avoid storing entire result objects — only log the id when available.
        Long retId = extractReturnId(result);
        if (retId != null) meta.put("returnedId", retId);
        return meta;
    }

    /** Builds a metadata map for failed invocations. */
    private Map<String, Object> buildFailureMetadata(ProceedingJoinPoint pjp, Throwable ex) {
        MethodSignature sig = (MethodSignature) pjp.getSignature();
        Map<String, Object> meta = new HashMap<>();
        meta.put("method", sig.getDeclaringType().getSimpleName() + "." + sig.getName());
        meta.put("exceptionType", ex.getClass().getSimpleName());
        return meta;
    }

    /**
     * Strips stack-trace details and limits length before storing in the DB.
     * Never log raw exception messages without sanitisation — they may leak
     * internal paths, SQL, or user data.
     */
    @Nullable
    private String sanitise(@Nullable String message) {
        if (message == null) return null;
        // Remove potential newline injection
        String clean = message.replaceAll("[\r\n\t]", " ").trim();
        return clean.length() > 1000 ? clean.substring(0, 1000) : clean;
    }
}
