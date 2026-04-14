package com.boatmanagement.exception;

import com.boatmanagement.audit.AuditContext;
import com.boatmanagement.entity.AuditAction;
import com.boatmanagement.service.AuditLogService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Global exception handler.
 *
 * Audit responsibilities here are intentionally narrow: the handler only
 * records
 * events that escape the @Auditable aspect — specifically access-denied
 * failures
 * thrown by Spring Security before a service method is even entered.
 *
 * Business-level failures (e.g. BoatNotFoundException from a service call) are
 * already captured as FAILURE records by AuditAspect, so we do NOT double-log
 * them.
 */
@RestControllerAdvice
@RequiredArgsConstructor
@Slf4j
public class GlobalExceptionHandler {

    private final AuditLogService auditLogService;

    /**
     * BoatNotFoundException is thrown inside @Auditable service methods,
     * so AuditAspect already captures it as FAILURE. No second audit entry here.
     * getMessage() is @NonNull on BoatNotFoundException (see that class).
     */
    @ExceptionHandler(BoatNotFoundException.class)
    public ProblemDetail handleBoatNotFound(BoatNotFoundException ex) {
        log.warn("Boat not found: {}", ex.getMessage());
        ProblemDetail detail = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        detail.setTitle("Boat Not Found");
        detail.setProperty("timestamp", Instant.now());
        return detail;
    }

    /**
     * Spring Security throws AccessDeniedException BEFORE reaching the service
     * layer,
     * so AuditAspect never sees it. We capture it here as an ACCESS_DENIED audit
     * event.
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ProblemDetail handleAccessDenied(AccessDeniedException ex) {
        log.warn("Access denied for user '{}': {}", AuditContext.currentUsername(), ex.getMessage());
        auditLogService.logFailure(
                AuditAction.ACCESS_DENIED,
                AuditContext.currentUsername(),
                null, null,
                ex.getMessage(),
                Map.of("reason", "Insufficient privileges", "exceptionType", ex.getClass().getSimpleName()),
                AuditContext.currentIpAddress(),
                AuditContext.currentUserAgent());
        ProblemDetail detail = ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, "Access denied");
        detail.setTitle("Forbidden");
        detail.setProperty("timestamp", Instant.now());
        return detail;
    }

    /**
     * Handles type conversion failures for @RequestParam and @PathVariable.
     * Thrown when e.g. ?status=INVALID is passed for a BoatStatus enum parameter.
     * Without this handler Spring returns a raw 400 without our ProblemDetail
     * format.
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ProblemDetail handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        String paramName = ex.getName();
        Object rejected = ex.getValue();
        Class<?> required = ex.getRequiredType();
        String expected = required != null ? required.getSimpleName() : "unknown";
        String detail = "Parameter '%s' has invalid value '%s'. Expected type: %s"
                .formatted(paramName, rejected, expected);
        log.warn("Type mismatch: {}", detail);
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, detail);
        pd.setTitle("Invalid Parameter");
        pd.setProperty("parameter", paramName);
        pd.setProperty("rejectedValue", rejected != null ? rejected.toString() : "null");
        pd.setProperty("timestamp", Instant.now());
        return pd;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidationErrors(MethodArgumentNotValidException ex) {
        log.warn("Validation error: {}", ex.getMessage());
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });

        ProblemDetail detail = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST, "Validation failed");
        detail.setTitle("Validation Error");
        detail.setProperty("errors", errors);
        detail.setProperty("timestamp", Instant.now());
        return detail;
    }

    @ExceptionHandler(org.springframework.http.converter.HttpMessageNotReadableException.class)
    public ProblemDetail handleMalformedJson(org.springframework.http.converter.HttpMessageNotReadableException ex) {
        log.warn("Malformed JSON request: {}", ex.getMessage());
        ProblemDetail detail = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST, "Malformed JSON request");
        detail.setTitle("Validation Error");
        detail.setProperty("timestamp", Instant.now());
        return detail;
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleGenericException(Exception ex) {
        log.error("Unexpected error", ex);
        ProblemDetail detail = ProblemDetail.forStatusAndDetail(
                HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred");
        detail.setTitle("Internal Server Error");
        detail.setProperty("timestamp", Instant.now());
        return detail;
    }
}
