package com.boatmanagement.audit;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Utility that extracts audit-relevant information from the current
 * Spring Security context and HTTP request.
 *
 * All methods are static and thread-safe — they read from thread-local
 * Spring holders and make no assumptions about bean lifecycle.
 *
 * Called from the AuditAspect (same thread as the service invocation)
 * BEFORE the async handoff, so the SecurityContext is still available.
 */
public final class AuditContext {

    private AuditContext() {}

    /**
     * Returns the authenticated user's preferred_username, falling back to
     * the JWT subject (UUID), or "anonymous" when no authentication is present.
     */
    public static String currentUsername() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth instanceof JwtAuthenticationToken jwtAuth) {
            String preferred = jwtAuth.getToken().getClaimAsString("preferred_username");
            return preferred != null ? preferred : jwtAuth.getToken().getSubject();
        }
        return "anonymous";
    }

    /**
     * Returns the client IP address from the current HTTP request,
     * honouring the X-Forwarded-For header set by the nginx reverse proxy.
     * Returns null when called outside an HTTP context (e.g., scheduled tasks).
     */
    public static String currentIpAddress() {
        HttpServletRequest request = currentRequest();
        if (request == null) return null;

        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            // XFF may be a comma-separated chain; the leftmost entry is the client.
            String first = xff.split(",")[0].trim();
            if (!first.isEmpty()) {
                return first;
            }
        }
        return request.getRemoteAddr();
    }

    /**
     * Returns the User-Agent header, truncated to 512 characters to match the
     * column length on the audit_log table.
     */
    public static String currentUserAgent() {
        HttpServletRequest request = currentRequest();
        if (request == null) return null;
        String ua = request.getHeader("User-Agent");
        if (ua == null) return null;
        return ua.length() > 512 ? ua.substring(0, 512) : ua;
    }

    private static HttpServletRequest currentRequest() {
        var attrs = RequestContextHolder.getRequestAttributes();
        if (attrs instanceof ServletRequestAttributes sra) {
            return sra.getRequest();
        }
        return null;
    }
}
