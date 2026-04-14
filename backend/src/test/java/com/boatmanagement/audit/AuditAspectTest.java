package com.boatmanagement.audit;

import com.boatmanagement.entity.AuditAction;
import com.boatmanagement.service.AuditLogService;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuditAspectTest {

    @Mock
    private AuditLogService auditLogService;

    @InjectMocks
    private AuditAspect auditAspect;

    @Mock
    private ProceedingJoinPoint pjp;

    @Mock
    private MethodSignature methodSignature;

    @BeforeEach
    void setUp() {
        // Bind a fake HTTP request so currentIpAddress / currentUserAgent don't NPE
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("127.0.0.1");
        request.addHeader("User-Agent", "TestAgent/1.0");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        // Set authenticated user
        Jwt jwt = Jwt.withTokenValue("tok")
                .header("alg", "RS256")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .claim("preferred_username", "testuser")
                .build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));

        // Wire up PJP → MethodSignature
        when(pjp.getSignature()).thenReturn(methodSignature);
        when(methodSignature.getDeclaringType()).thenAnswer(inv -> SomeService.class);
        when(methodSignature.getName()).thenReturn("doSomething");
    }

    @org.junit.jupiter.api.AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        RequestContextHolder.resetRequestAttributes();
    }

    // -----------------------------------------------------------------------
    // Success path
    // -----------------------------------------------------------------------
    @Nested
    class SuccessPath {

        @Test
        void callsLogSuccessAndReturnsResult() throws Throwable {
            // Arrange
            Auditable auditable = buildAnnotation(AuditAction.BOAT_LIST, "BOAT", -1);
            String returned = "result";
            when(pjp.getArgs()).thenReturn(new Object[0]);
            when(pjp.proceed()).thenReturn(returned);

            // Act
            Object actual = auditAspect.audit(pjp, auditable);

            // Assert
            assertThat(actual).isEqualTo(returned);
            verify(auditLogService).logSuccess(
                    eq(AuditAction.BOAT_LIST), eq("testuser"), eq("BOAT"),
                    isNull(), any(), eq("127.0.0.1"), eq("TestAgent/1.0"));
            verify(auditLogService, never()).logFailure(any(), any(), any(), any(), any(), any(), any(), any());
        }

        @Test
        void extractsResourceIdFromArgWhenIndexIsZero() throws Throwable {
            // Arrange — resourceIdArgIndex = 0, first arg is Long 42
            Auditable auditable = buildAnnotation(AuditAction.BOAT_READ, "BOAT", 0);
            when(pjp.getArgs()).thenReturn(new Object[]{42L});
            when(pjp.proceed()).thenReturn(null);

            // Act
            auditAspect.audit(pjp, auditable);

            // Assert — id must be 42 and not extracted from return value
            verify(auditLogService).logSuccess(
                    eq(AuditAction.BOAT_READ), eq("testuser"), eq("BOAT"),
                    eq(42L), any(), any(), any());
        }

        @Test
        void extractsResourceIdFromReturnValueWhenArgIndexIsMinusOne() throws Throwable {
            // Arrange — no id in args; return value has getId() → 7L
            Auditable auditable = buildAnnotation(AuditAction.BOAT_CREATE, "BOAT", -1);
            when(pjp.getArgs()).thenReturn(new Object[0]);
            DtoWithId dto = new DtoWithId(7L);
            when(pjp.proceed()).thenReturn(dto);

            // Act
            auditAspect.audit(pjp, auditable);

            // Assert
            verify(auditLogService).logSuccess(
                    eq(AuditAction.BOAT_CREATE), eq("testuser"), eq("BOAT"),
                    eq(7L), any(), any(), any());
        }

        @Test
        void setsNullResourceIdWhenArgIndexOutOfBounds() throws Throwable {
            // Arrange — index 5 but args has only 1 element
            Auditable auditable = buildAnnotation(AuditAction.BOAT_READ, "BOAT", 5);
            when(pjp.getArgs()).thenReturn(new Object[]{42L});
            when(pjp.proceed()).thenReturn(null);

            // Act
            auditAspect.audit(pjp, auditable);

            // Assert — id falls through to null
            verify(auditLogService).logSuccess(
                    eq(AuditAction.BOAT_READ), eq("testuser"), eq("BOAT"),
                    isNull(), any(), any(), any());
        }

        @Test
        void setsNullResourceTypeWhenAnnotationResourceTypeIsBlank() throws Throwable {
            // Arrange — resourceType defaults to ""
            Auditable auditable = buildAnnotation(AuditAction.BOAT_LIST, "", -1);
            when(pjp.getArgs()).thenReturn(new Object[0]);
            when(pjp.proceed()).thenReturn(null);

            // Act
            auditAspect.audit(pjp, auditable);

            // Assert — blank converted to null
            verify(auditLogService).logSuccess(
                    any(), any(), isNull(), any(), any(), any(), any());
        }

        @Test
        void includesMethodNameInSuccessMetadata() throws Throwable {
            // Arrange
            Auditable auditable = buildAnnotation(AuditAction.BOAT_LIST, "BOAT", -1);
            when(pjp.getArgs()).thenReturn(new Object[0]);
            when(pjp.proceed()).thenReturn(null);

            // Act
            auditAspect.audit(pjp, auditable);

            // Assert — metadata map contains "method" key
            ArgumentCaptor<Map<String, Object>> metaCaptor = metaCaptor();
            verify(auditLogService).logSuccess(any(), any(), any(), any(), metaCaptor.capture(), any(), any());
            assertThat(metaCaptor.getValue()).containsKey("method");
            assertThat(metaCaptor.getValue().get("method").toString())
                    .contains("SomeService").contains("doSomething");
        }
    }

    // -----------------------------------------------------------------------
    // Failure path
    // -----------------------------------------------------------------------
    @Nested
    class FailurePath {

        @Test
        void callsLogFailureAndRethrowsException() throws Throwable {
            // Arrange
            Auditable auditable = buildAnnotation(AuditAction.BOAT_READ, "BOAT", 0);
            RuntimeException cause = new RuntimeException("not found");
            when(pjp.getArgs()).thenReturn(new Object[]{99L});
            when(pjp.proceed()).thenThrow(cause);

            // Act & Assert
            assertThatThrownBy(() -> auditAspect.audit(pjp, auditable))
                    .isSameAs(cause);

            verify(auditLogService).logFailure(
                    eq(AuditAction.BOAT_READ), eq("testuser"), eq("BOAT"),
                    eq(99L), eq("not found"), any(), any(), any());
            verify(auditLogService, never()).logSuccess(any(), any(), any(), any(), any(), any(), any());
        }

        @Test
        void sanitisesExceptionMessageBeforeLogging() throws Throwable {
            // Arrange — message contains newlines that could pollute logs
            Auditable auditable = buildAnnotation(AuditAction.BOAT_DELETE, "BOAT", 0);
            RuntimeException cause = new RuntimeException("line1\nline2\r\nline3\ttab");
            when(pjp.getArgs()).thenReturn(new Object[]{1L});
            when(pjp.proceed()).thenThrow(cause);

            // Act
            assertThatThrownBy(() -> auditAspect.audit(pjp, auditable)).isSameAs(cause);

            // Assert — message is sanitised (no CR/LF/tab)
            ArgumentCaptor<String> errorCaptor = ArgumentCaptor.forClass(String.class);
            verify(auditLogService).logFailure(any(), any(), any(), any(),
                    errorCaptor.capture(), any(), any(), any());
            assertThat(errorCaptor.getValue()).doesNotContain("\n", "\r", "\t");
        }

        @Test
        void truncatesSanitisedMessageAt1000Chars() throws Throwable {
            // Arrange
            Auditable auditable = buildAnnotation(AuditAction.BOAT_UPDATE, "BOAT", 0);
            RuntimeException cause = new RuntimeException("X".repeat(1500));
            when(pjp.getArgs()).thenReturn(new Object[]{1L});
            when(pjp.proceed()).thenThrow(cause);

            // Act
            assertThatThrownBy(() -> auditAspect.audit(pjp, auditable)).isSameAs(cause);

            // Assert
            ArgumentCaptor<String> errorCaptor = ArgumentCaptor.forClass(String.class);
            verify(auditLogService).logFailure(any(), any(), any(), any(),
                    errorCaptor.capture(), any(), any(), any());
            assertThat(errorCaptor.getValue()).hasSize(1000);
        }

        @Test
        void handlesNullExceptionMessage() throws Throwable {
            // Arrange — exception whose getMessage() returns null
            Auditable auditable = buildAnnotation(AuditAction.BOAT_DELETE, "BOAT", 0);
            RuntimeException cause = new RuntimeException((String) null);
            when(pjp.getArgs()).thenReturn(new Object[]{1L});
            when(pjp.proceed()).thenThrow(cause);

            // Act
            assertThatThrownBy(() -> auditAspect.audit(pjp, auditable)).isSameAs(cause);

            // Assert — null message passed through as null
            ArgumentCaptor<String> errorCaptor = ArgumentCaptor.forClass(String.class);
            verify(auditLogService).logFailure(any(), any(), any(), any(),
                    errorCaptor.capture(), any(), any(), any());
            assertThat(errorCaptor.getValue()).isNull();
        }

        @Test
        void includesExceptionTypeInFailureMetadata() throws Throwable {
            // Arrange
            Auditable auditable = buildAnnotation(AuditAction.BOAT_READ, "BOAT", 0);
            IllegalStateException cause = new IllegalStateException("boom");
            when(pjp.getArgs()).thenReturn(new Object[]{1L});
            when(pjp.proceed()).thenThrow(cause);

            // Act
            assertThatThrownBy(() -> auditAspect.audit(pjp, auditable)).isSameAs(cause);

            // Assert — metadata includes exception type
            ArgumentCaptor<Map<String, Object>> metaCaptor = metaCaptor();
            verify(auditLogService).logFailure(any(), any(), any(), any(), any(),
                    metaCaptor.capture(), any(), any());
            assertThat(metaCaptor.getValue()).containsEntry("exceptionType", "IllegalStateException");
        }
    }

    // -----------------------------------------------------------------------
    // extractArgId edge cases
    // -----------------------------------------------------------------------
    @Nested
    class ExtractArgId {

        @Test
        void acceptsIntegerArgAndConvertsToLong() throws Throwable {
            // Arrange — arg is an Integer (not Long)
            Auditable auditable = buildAnnotation(AuditAction.BOAT_READ, "BOAT", 0);
            when(pjp.getArgs()).thenReturn(new Object[]{Integer.valueOf(77)});
            when(pjp.proceed()).thenReturn(null);

            // Act
            auditAspect.audit(pjp, auditable);

            // Assert
            verify(auditLogService).logSuccess(any(), any(), any(), eq(77L), any(), any(), any());
        }

        @Test
        void returnsNullForNonNumericArg() throws Throwable {
            // Arrange — arg at index 0 is a String, not a number
            Auditable auditable = buildAnnotation(AuditAction.BOAT_READ, "BOAT", 0);
            when(pjp.getArgs()).thenReturn(new Object[]{"notAnId"});
            when(pjp.proceed()).thenReturn(null);

            // Act
            auditAspect.audit(pjp, auditable);

            // Assert — resourceId is null (String is not Long or Integer)
            verify(auditLogService).logSuccess(any(), any(), any(), isNull(), any(), any(), any());
        }

        @Test
        void returnsNullWhenArgsArrayIsEmpty() throws Throwable {
            // Arrange
            Auditable auditable = buildAnnotation(AuditAction.BOAT_READ, "BOAT", 0);
            when(pjp.getArgs()).thenReturn(new Object[0]);
            when(pjp.proceed()).thenReturn(null);

            // Act
            auditAspect.audit(pjp, auditable);

            // Assert
            verify(auditLogService).logSuccess(any(), any(), any(), isNull(), any(), any(), any());
        }
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    /** Minimal helper so tests can construct @Auditable instances without reflection. */
    private static Auditable buildAnnotation(AuditAction action, String resourceType, int idArgIndex) {
        return new Auditable() {
            public Class<? extends java.lang.annotation.Annotation> annotationType() { return Auditable.class; }
            public AuditAction action()           { return action; }
            public String resourceType()          { return resourceType; }
            public int resourceIdArgIndex()       { return idArgIndex; }
        };
    }

    @SuppressWarnings("unchecked")
    private static <T> ArgumentCaptor<T> metaCaptor() {
        return (ArgumentCaptor<T>) ArgumentCaptor.forClass(Map.class);
    }

    /** Fake service class name to match in metadata assertions. */
    static class SomeService {}

    /** Fake DTO that exposes getId() — mirrors BoatDto.Response. */
    static class DtoWithId {
        private final Long id;
        DtoWithId(Long id) { this.id = id; }
        public Long getId() { return id; }
    }
}
