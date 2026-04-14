package com.boatmanagement.audit;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class AuditContextTest {

    @BeforeEach
    void clearContexts() {
        SecurityContextHolder.clearContext();
        RequestContextHolder.resetRequestAttributes();
    }

    @AfterEach
    void resetContexts() {
        SecurityContextHolder.clearContext();
        RequestContextHolder.resetRequestAttributes();
    }

    // -----------------------------------------------------------------------
    // currentUsername
    // -----------------------------------------------------------------------
    @Nested
    class CurrentUsername {

        @Test
        void returnsPreferredUsernameFromJwtWhenPresent() {
            // Arrange
            Jwt jwt = buildJwt(Map.of("preferred_username", "alice", "sub", "uuid-001"));
            SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));

            // Act & Assert
            assertThat(AuditContext.currentUsername()).isEqualTo("alice");
        }

        @Test
        void fallsBackToJwtSubjectWhenPreferredUsernameIsAbsent() {
            // Arrange — no preferred_username claim
            Jwt jwt = buildJwt(Map.of("sub", "uuid-001"));
            SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));

            // Act & Assert
            assertThat(AuditContext.currentUsername()).isEqualTo("uuid-001");
        }

        @Test
        void returnsAnonymousWhenNoAuthentication() {
            // Arrange — SecurityContext has no authentication (clearContext in @BeforeEach)

            // Act & Assert
            assertThat(AuditContext.currentUsername()).isEqualTo("anonymous");
        }

        @Test
        void returnsAnonymousForNonJwtAuthentication() {
            // Arrange — a non-JWT auth (e.g. username/password)
            SecurityContextHolder.getContext()
                    .setAuthentication(new TestingAuthenticationToken("bob", "password"));

            // Act & Assert
            assertThat(AuditContext.currentUsername()).isEqualTo("anonymous");
        }
    }

    // -----------------------------------------------------------------------
    // currentIpAddress
    // -----------------------------------------------------------------------
    @Nested
    class CurrentIpAddress {

        @Test
        void returnsNullOutsideHttpContext() {
            // Arrange — no request attributes

            // Act & Assert
            assertThat(AuditContext.currentIpAddress()).isNull();
        }

        @Test
        void returnsRemoteAddrWhenXffAbsent() {
            // Arrange
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.setRemoteAddr("10.0.0.1");
            bindRequest(request);

            // Act & Assert
            assertThat(AuditContext.currentIpAddress()).isEqualTo("10.0.0.1");
        }

        @Test
        void returnsFirstEntryOfXffHeader() {
            // Arrange — XFF contains a chain of proxies
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.addHeader("X-Forwarded-For", "203.0.113.5, 10.1.2.3, 172.16.0.1");
            bindRequest(request);

            // Act & Assert
            assertThat(AuditContext.currentIpAddress()).isEqualTo("203.0.113.5");
        }

        @Test
        void returnsXffWhenItContainsOnlyOneEntry() {
            // Arrange
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.addHeader("X-Forwarded-For", "1.2.3.4");
            bindRequest(request);

            // Act & Assert
            assertThat(AuditContext.currentIpAddress()).isEqualTo("1.2.3.4");
        }

        @Test
        void fallsBackToRemoteAddrWhenXffIsBlank() {
            // Arrange
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.addHeader("X-Forwarded-For", "   ");
            request.setRemoteAddr("10.9.8.7");
            bindRequest(request);

            // Act & Assert
            assertThat(AuditContext.currentIpAddress()).isEqualTo("10.9.8.7");
        }
    }

    // -----------------------------------------------------------------------
    // currentUserAgent
    // -----------------------------------------------------------------------
    @Nested
    class CurrentUserAgent {

        @Test
        void returnsNullOutsideHttpContext() {
            // Arrange — no request attributes

            // Act & Assert
            assertThat(AuditContext.currentUserAgent()).isNull();
        }

        @Test
        void returnsNullWhenUserAgentHeaderAbsent() {
            // Arrange
            bindRequest(new MockHttpServletRequest());

            // Act & Assert
            assertThat(AuditContext.currentUserAgent()).isNull();
        }

        @Test
        void returnsUserAgentWhenWithin512Chars() {
            // Arrange
            MockHttpServletRequest request = new MockHttpServletRequest();
            String ua = "Mozilla/5.0 (Windows NT 10.0; Win64; x64)";
            request.addHeader("User-Agent", ua);
            bindRequest(request);

            // Act & Assert
            assertThat(AuditContext.currentUserAgent()).isEqualTo(ua);
        }

        @Test
        void truncatesUserAgentExceeding512Chars() {
            // Arrange — UA that is 513 chars
            MockHttpServletRequest request = new MockHttpServletRequest();
            String longUa = "A".repeat(513);
            request.addHeader("User-Agent", longUa);
            bindRequest(request);

            // Act
            String result = AuditContext.currentUserAgent();

            // Assert
            assertThat(result).isNotNull().hasSize(512);
            assertThat(result).isEqualTo("A".repeat(512));
        }

        @Test
        void doesNotTruncateUserAgentOfExactly512Chars() {
            // Arrange
            MockHttpServletRequest request = new MockHttpServletRequest();
            String exactUa = "B".repeat(512);
            request.addHeader("User-Agent", exactUa);
            bindRequest(request);

            // Act & Assert
            assertThat(AuditContext.currentUserAgent()).hasSize(512);
        }
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private static Jwt buildJwt(Map<String, Object> claims) {
        return Jwt.withTokenValue("test-token")
                .header("alg", "RS256")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .claims(c -> c.putAll(claims))
                .build();
    }

    private static void bindRequest(HttpServletRequest request) {
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
    }
}
