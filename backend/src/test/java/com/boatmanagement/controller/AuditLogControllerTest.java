package com.boatmanagement.controller;

import com.boatmanagement.entity.AuditAction;
import com.boatmanagement.entity.AuditLog;
import com.boatmanagement.repository.AuditLogRepository;
import com.boatmanagement.service.AuditLogService;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuditLogController.class)
@Import(com.boatmanagement.config.SecurityConfig.class)
class AuditLogControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AuditLogRepository auditLogRepository;

    // Prevents Spring Boot from trying to fetch the JWKS from Keycloak at startup
    @MockBean
    private JwtDecoder jwtDecoder;

    // GlobalExceptionHandler injects AuditLogService via @RequiredArgsConstructor
    @MockBean
    private AuditLogService auditLogService;

    // -----------------------------------------------------------------------
    // GET /api/audit-logs — access control
    // -----------------------------------------------------------------------
    @Nested
    class SearchAccessControl {

        @Test
        void returns401WhenNotAuthenticated() throws Exception {
            mockMvc.perform(get("/api/audit-logs"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @WithMockUser(username = "user", roles = {"USER"})
        void returns403ForNonAdminUser() throws Exception {
            mockMvc.perform(get("/api/audit-logs"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser(username = "admin", roles = {"ADMIN"})
        void returns200ForAdminUser() throws Exception {
            when(auditLogRepository.search(any(), any(), any(), any(), any(), any(), any(Pageable.class)))
                    .thenReturn(new PageImpl<>(List.of()));

            mockMvc.perform(get("/api/audit-logs"))
                    .andExpect(status().isOk());
        }
    }

    // -----------------------------------------------------------------------
    // GET /api/audit-logs — search results
    // -----------------------------------------------------------------------
    @Nested
    class Search {

        @Test
        @WithMockUser(username = "admin", roles = {"ADMIN"})
        void returnsPagedResultsWithAllMetadata() throws Exception {
            // Arrange
            AuditLog entry = buildAuditLog(1L, "alice", AuditAction.BOAT_CREATE, "BOAT",
                    42L, AuditLog.Outcome.SUCCESS);
            when(auditLogRepository.search(any(), any(), any(), any(), any(), any(), any(Pageable.class)))
                    .thenReturn(new PageImpl<>(List.of(entry)));

            // Act & Assert
            mockMvc.perform(get("/api/audit-logs"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalElements").value(1))
                    .andExpect(jsonPath("$.content[0].username").value("alice"))
                    .andExpect(jsonPath("$.content[0].action").value("BOAT_CREATE"))
                    .andExpect(jsonPath("$.content[0].resourceType").value("BOAT"))
                    .andExpect(jsonPath("$.content[0].resourceId").value(42))
                    .andExpect(jsonPath("$.content[0].outcome").value("SUCCESS"));
        }

        @Test
        @WithMockUser(username = "admin", roles = {"ADMIN"})
        void returnsEmptyPageWhenNoResults() throws Exception {
            when(auditLogRepository.search(any(), any(), any(), any(), any(), any(), any(Pageable.class)))
                    .thenReturn(new PageImpl<>(List.of()));

            mockMvc.perform(get("/api/audit-logs"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalElements").value(0))
                    .andExpect(jsonPath("$.content").isEmpty());
        }

        @Test
        @WithMockUser(username = "admin", roles = {"ADMIN"})
        void capsPageSizeAt100() throws Exception {
            // Arrange — client requests size=200
            when(auditLogRepository.search(any(), any(), any(), any(), any(), any(), any(Pageable.class)))
                    .thenReturn(new PageImpl<>(List.of()));

            // Act & Assert — the controller silently caps to 100
            mockMvc.perform(get("/api/audit-logs").param("size", "200"))
                    .andExpect(status().isOk());

            // Verify the pageable passed to repository has size ≤ 100
            org.mockito.ArgumentCaptor<Pageable> pageableCaptor =
                    org.mockito.ArgumentCaptor.forClass(Pageable.class);
            org.mockito.Mockito.verify(auditLogRepository)
                    .search(any(), any(), any(), any(), any(), any(), pageableCaptor.capture());
            org.assertj.core.api.Assertions
                    .assertThat(pageableCaptor.getValue().getPageSize()).isLessThanOrEqualTo(100);
        }
    }

    // -----------------------------------------------------------------------
    // GET /api/audit-logs/{id}
    // -----------------------------------------------------------------------
    @Nested
    class GetById {

        @Test
        void returns401WhenNotAuthenticated() throws Exception {
            mockMvc.perform(get("/api/audit-logs/1"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @WithMockUser(username = "user", roles = {"USER"})
        void returns403ForNonAdminUser() throws Exception {
            mockMvc.perform(get("/api/audit-logs/1"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser(username = "admin", roles = {"ADMIN"})
        void returns200WithDtoWhenFound() throws Exception {
            // Arrange
            AuditLog entry = buildAuditLog(5L, "bob", AuditAction.BOAT_DELETE, "BOAT",
                    10L, AuditLog.Outcome.FAILURE);
            when(auditLogRepository.findById(5L)).thenReturn(Optional.of(entry));

            // Act & Assert
            mockMvc.perform(get("/api/audit-logs/5"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(5))
                    .andExpect(jsonPath("$.username").value("bob"))
                    .andExpect(jsonPath("$.action").value("BOAT_DELETE"))
                    .andExpect(jsonPath("$.outcome").value("FAILURE"));
        }

        @Test
        @WithMockUser(username = "admin", roles = {"ADMIN"})
        void returns404WhenNotFound() throws Exception {
            when(auditLogRepository.findById(999L)).thenReturn(Optional.empty());

            mockMvc.perform(get("/api/audit-logs/999"))
                    .andExpect(status().isNotFound());
        }
    }

    // -----------------------------------------------------------------------
    // Helper
    // -----------------------------------------------------------------------

    private static AuditLog buildAuditLog(Long id, String username, AuditAction action,
                                          String resourceType, Long resourceId, AuditLog.Outcome outcome) {
        return AuditLog.builder()
                .id(id)
                .username(username)
                .action(action)
                .resourceType(resourceType)
                .resourceId(resourceId)
                .outcome(outcome)
                .occurredAt(Instant.now())
                .metadata(Map.of("method", "BoatService.create"))
                .build();
    }
}
