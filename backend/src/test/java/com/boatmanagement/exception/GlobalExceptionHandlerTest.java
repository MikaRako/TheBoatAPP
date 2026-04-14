package com.boatmanagement.exception;

import com.boatmanagement.controller.BoatController;
import com.boatmanagement.entity.AuditAction;
import com.boatmanagement.service.AuditLogService;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Tests GlobalExceptionHandler via @WebMvcTest so the full MVC dispatcher
 * and exception-resolver chain is exercised without starting a server.
 *
 * BoatController is used as the web layer trigger because it is already wired
 * with all the exception paths we need:
 * - POST /api/boats with bad body → MethodArgumentNotValidException → 400
 * - GET /api/boats?status=INVALID → MethodArgumentTypeMismatchException → 400
 * - GET /api/boats/{id} with non-existent id → BoatNotFoundException → 404
 * - GET /api/audit-logs (non-admin) → AccessDeniedException → 403
 */
@WebMvcTest(BoatController.class)
@Import(com.boatmanagement.config.SecurityConfig.class)
class GlobalExceptionHandlerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private com.boatmanagement.service.BoatService boatService;

    // Prevents Spring Boot from trying to fetch the JWKS from Keycloak at startup
    @MockBean
    private JwtDecoder jwtDecoder;

    @MockBean
    private AuditLogService auditLogService;

    // -----------------------------------------------------------------------
    // BoatNotFoundException → 404
    // -----------------------------------------------------------------------
    @Nested
    class BoatNotFound {

        @Test
        @WithMockUser(username = "user")
        void returns404WithProblemDetail() throws Exception {
            when(boatService.findById(999L))
                    .thenThrow(new BoatNotFoundException(999L));

            mockMvc.perform(get("/api/boats/999"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.title").value("Boat Not Found"))
                    .andExpect(jsonPath("$.timestamp").exists());
        }
    }

    // -----------------------------------------------------------------------
    // MethodArgumentNotValidException → 400
    // -----------------------------------------------------------------------
    @Nested
    class ValidationError {

        @Test
        @WithMockUser(username = "user")
        void returns400WithFieldErrorsWhenBodyIsInvalid() throws Exception {
            // Arrange — empty name violates @NotBlank, missing status/type violate @NotNull
            String json = "{\"name\": \"\"}";

            mockMvc.perform(post("/api/boats")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(json))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.title").value("Validation Error"))
                    .andExpect(jsonPath("$.errors").exists())
                    .andExpect(jsonPath("$.timestamp").exists());
        }

        @Test
        @WithMockUser(username = "user")
        void returns400ForMalformedJson() throws Exception {
            mockMvc.perform(post("/api/boats")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{not-valid-json"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.title").value("Validation Error"));
        }
    }

    // -----------------------------------------------------------------------
    // MethodArgumentTypeMismatchException → 400
    // -----------------------------------------------------------------------
    @Nested
    class TypeMismatch {

        @Test
        @WithMockUser(username = "user")
        void returns400WithInvalidParameterTitleForBadStatusEnum() throws Exception {
            mockMvc.perform(get("/api/boats").param("status", "FLYING"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.title").value("Invalid Parameter"))
                    .andExpect(jsonPath("$.parameter").value("status"))
                    .andExpect(jsonPath("$.rejectedValue").value("FLYING"))
                    .andExpect(jsonPath("$.timestamp").exists());
        }

        @Test
        @WithMockUser(username = "user")
        void returns400WithInvalidParameterTitleForBadTypeEnum() throws Exception {
            mockMvc.perform(get("/api/boats").param("type", "SUBMARINE"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.title").value("Invalid Parameter"))
                    .andExpect(jsonPath("$.parameter").value("type"))
                    .andExpect(jsonPath("$.rejectedValue").value("SUBMARINE"));
        }
    }

    // -----------------------------------------------------------------------
    // AccessDeniedException → 403 + audit
    // -----------------------------------------------------------------------
    @Nested
    class AccessDenied {

        @Test
        @WithMockUser(username = "user", roles = { "USER" })
        void returns403AndAuditsAccessDeniedEvent() throws Exception {
            // Arrange — /api/boats is accessible to authenticated users, but
            // we can trigger a 403 via a method protected by @PreAuthorize.
            // Use an endpoint that requires ADMIN — simulate with a direct
            // call to an admin-only path via BoatController (which has none).
            // Instead we verify the AuditLogService is called by the handler
            // when Spring Security rejects the request before it reaches the
            // controller: here we force this by throwing AccessDeniedException
            // from the service.
            when(boatService.findAll(any(), any(), any(), anyInt(), anyInt(), any(), any()))
                    .thenThrow(new org.springframework.security.access.AccessDeniedException("Forbidden"));

            mockMvc.perform(get("/api/boats"))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.title").value("Forbidden"))
                    .andExpect(jsonPath("$.timestamp").exists());

            // Verify audit event was recorded
            verify(auditLogService).logFailure(
                    eq(AuditAction.ACCESS_DENIED),
                    any(), isNull(), isNull(),
                    eq("Forbidden"),   // ex.getMessage() from the thrown AccessDeniedException
                    any(), any(), any());
        }
    }
}
