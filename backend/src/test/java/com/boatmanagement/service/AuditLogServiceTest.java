package com.boatmanagement.service;

import com.boatmanagement.entity.AuditAction;
import com.boatmanagement.entity.AuditLog;
import com.boatmanagement.repository.AuditLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuditLogServiceTest {

    @Mock
    private AuditLogRepository auditLogRepository;

    @InjectMocks
    private AuditLogService auditLogService;

    // Capture what is actually saved
    private final ArgumentCaptor<AuditLog> logCaptor = ArgumentCaptor.forClass(AuditLog.class);

    @BeforeEach
    void setUp() {
        // Default stub: save() echoes back the entity.
        // Exception tests override this with lenient() to avoid UnnecessaryStubbingException.
        lenient().when(auditLogRepository.save(any(AuditLog.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    // -----------------------------------------------------------------------
    // logSuccess
    // -----------------------------------------------------------------------
    @Nested
    class LogSuccess {

        @Test
        void persistsSuccessRecordWithAllFields() {
            // Arrange
            Map<String, Object> meta = Map.of("key", "value");

            // Act
            auditLogService.logSuccess(
                    AuditAction.BOAT_CREATE, "alice", "BOAT", 42L, meta, "10.0.0.1", "Mozilla/5.0");

            // Assert
            verify(auditLogRepository).save(logCaptor.capture());
            AuditLog saved = logCaptor.getValue();
            assertThat(saved.getAction()).isEqualTo(AuditAction.BOAT_CREATE);
            assertThat(saved.getUsername()).isEqualTo("alice");
            assertThat(saved.getResourceType()).isEqualTo("BOAT");
            assertThat(saved.getResourceId()).isEqualTo(42L);
            assertThat(saved.getOutcome()).isEqualTo(AuditLog.Outcome.SUCCESS);
            assertThat(saved.getErrorMessage()).isNull();
            assertThat(saved.getMetadata()).isEqualTo(meta);
            assertThat(saved.getIpAddress()).isEqualTo("10.0.0.1");
            assertThat(saved.getUserAgent()).isEqualTo("Mozilla/5.0");
            assertThat(saved.getOccurredAt()).isNotNull();
        }

        @Test
        void persistsSuccessRecordWithNullOptionalFields() {
            // Arrange / Act
            auditLogService.logSuccess(
                    AuditAction.BOAT_LIST, "bob", null, null, null, null, null);

            // Assert
            verify(auditLogRepository).save(logCaptor.capture());
            AuditLog saved = logCaptor.getValue();
            assertThat(saved.getOutcome()).isEqualTo(AuditLog.Outcome.SUCCESS);
            assertThat(saved.getResourceType()).isNull();
            assertThat(saved.getResourceId()).isNull();
            assertThat(saved.getMetadata()).isNull();
            assertThat(saved.getIpAddress()).isNull();
            assertThat(saved.getUserAgent()).isNull();
        }
    }

    // -----------------------------------------------------------------------
    // logFailure
    // -----------------------------------------------------------------------
    @Nested
    class LogFailure {

        @Test
        void persistsFailureRecordWithErrorMessage() {
            // Arrange
            Map<String, Object> meta = Map.of("exceptionType", "BoatNotFoundException");

            // Act
            auditLogService.logFailure(
                    AuditAction.BOAT_READ, "carol", "BOAT", 99L,
                    "Boat not found: 99", meta, "192.168.1.1", "curl/7.0");

            // Assert
            verify(auditLogRepository).save(logCaptor.capture());
            AuditLog saved = logCaptor.getValue();
            assertThat(saved.getOutcome()).isEqualTo(AuditLog.Outcome.FAILURE);
            assertThat(saved.getErrorMessage()).isEqualTo("Boat not found: 99");
            assertThat(saved.getAction()).isEqualTo(AuditAction.BOAT_READ);
            assertThat(saved.getResourceId()).isEqualTo(99L);
        }

        @Test
        void persistsFailureRecordWithNullErrorMessage() {
            // Act
            auditLogService.logFailure(
                    AuditAction.ACCESS_DENIED, "dave", null, null, null, null, null, null);

            // Assert
            verify(auditLogRepository).save(logCaptor.capture());
            AuditLog saved = logCaptor.getValue();
            assertThat(saved.getOutcome()).isEqualTo(AuditLog.Outcome.FAILURE);
            assertThat(saved.getErrorMessage()).isNull();
        }

        @Test
        void truncatesErrorMessageExceeding1000Chars() {
            // Arrange — a message that is exactly 1001 chars
            String longMessage = "E".repeat(1001);

            // Act
            auditLogService.logFailure(
                    AuditAction.BOAT_DELETE, "eve", "BOAT", 1L,
                    longMessage, null, null, null);

            // Assert
            verify(auditLogRepository).save(logCaptor.capture());
            AuditLog saved = logCaptor.getValue();
            assertThat(saved.getErrorMessage()).hasSize(1000);
            assertThat(saved.getErrorMessage()).isEqualTo("E".repeat(1000));
        }

        @Test
        void doesNotTruncateErrorMessageOfExactly1000Chars() {
            // Arrange
            String exactMessage = "X".repeat(1000);

            // Act
            auditLogService.logFailure(
                    AuditAction.BOAT_UPDATE, "eve", "BOAT", 1L,
                    exactMessage, null, null, null);

            // Assert
            verify(auditLogRepository).save(logCaptor.capture());
            AuditLog saved = logCaptor.getValue();
            assertThat(saved.getErrorMessage()).hasSize(1000);
        }
    }

    // -----------------------------------------------------------------------
    // Exception swallowing
    // -----------------------------------------------------------------------
    @Nested
    class ExceptionSwallowing {

        @Test
        void doesNotPropagateWhenRepositoryThrows() {
            // Arrange — override the default stub so save() throws
            lenient().doThrow(new RuntimeException("DB down")).when(auditLogRepository).save(any());

            // Act & Assert — no exception must escape the service
            assertThatNoException().isThrownBy(() ->
                    auditLogService.logSuccess(
                            AuditAction.BOAT_CREATE, "frank", "BOAT", 1L, null, null, null));
        }

        @Test
        void doesNotPropagateWhenRepositoryThrowsOnFailurePath() {
            // Arrange
            lenient().doThrow(new RuntimeException("DB down")).when(auditLogRepository).save(any());

            // Act & Assert
            assertThatNoException().isThrownBy(() ->
                    auditLogService.logFailure(
                            AuditAction.BOAT_DELETE, "grace", "BOAT", 5L,
                            "something went wrong", null, null, null));
        }

        @Test
        void stillAttemptsToSaveOnSubsequentCallAfterPreviousFailure() {
            // Arrange — first call throws, second succeeds
            lenient().doThrow(new RuntimeException("transient"))
                    .doAnswer(inv -> inv.getArgument(0))
                    .when(auditLogRepository).save(any());

            auditLogService.logSuccess(AuditAction.BOAT_LIST, "henry", null, null, null, null, null);
            auditLogService.logSuccess(AuditAction.BOAT_LIST, "henry", null, null, null, null, null);

            // Both calls were attempted
            verify(auditLogRepository, times(2)).save(any());
        }
    }
}
