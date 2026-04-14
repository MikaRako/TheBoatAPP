package com.boatmanagement.audit;

import com.boatmanagement.entity.AuditAction;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a service method for audit logging.
 *
 * The AuditAspect intercepts any method annotated with @Auditable and
 * automatically records a SUCCESS or FAILURE entry in the audit_log table.
 *
 * Usage:
 * <pre>
 *   @Auditable(action = AuditAction.BOAT_CREATE, resourceType = "BOAT")
 *   public BoatDto.Response create(BoatDto.Request request) { ... }
 * </pre>
 *
 * resourceIdArgIndex: zero-based index of the method parameter that holds the
 * resource's primary key.  Set to -1 (default) when there is no such parameter
 * (e.g., create operations where the id is only known after the call succeeds).
 * For those cases the aspect extracts the id from the return value instead.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Auditable {

    AuditAction action();

    /** Logical resource name stored in audit_log.resource_type (e.g. "BOAT"). */
    String resourceType() default "";

    /**
     * Zero-based index of the method argument that holds the resource id.
     * -1 means "no id parameter" — the aspect will try to read it from the
     * return value (expects a DTO with a getId() method).
     */
    int resourceIdArgIndex() default -1;
}
