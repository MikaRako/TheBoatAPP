package com.boatmanagement.controller;

import com.boatmanagement.dto.BoatDto;
import com.boatmanagement.exception.BoatNotFoundException;
import com.boatmanagement.service.BoatService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Testing strategy — Input Adapter (REST):
 *
 * @WebMvcTest loads only the web layer: BoatController, GlobalExceptionHandler,
 *             Jackson, and Spring Security. BoatService is mocked (input port
 *             boundary).
 *
 *             JwtDecoder is @MockBean'd to prevent Spring from trying to
 *             resolve the
 *             Keycloak issuer URI during context startup.
 *
 *             Each @Nested class covers one endpoint. Tests verify:
 *             - HTTP status codes (normal, 400, 401, 404)
 *             - Response body shape (JSON fields, types)
 *             - Bean Validation enforcement (blank, null, boundary lengths,
 *             whitespace)
 *             - Security: authenticated vs unauthenticated access
 *             - Service delegation: correct parameters are forwarded
 */
@WebMvcTest(BoatController.class)
@DisplayName("BoatController — REST input adapter tests")
class BoatControllerTest {

        @Autowired
        private MockMvc mockMvc;

        @Autowired
        private ObjectMapper objectMapper;

        @MockBean
        private BoatService boatService;

        // Prevents Spring Boot from trying to fetch the JWKS from Keycloak at startup
        @MockBean
        private JwtDecoder jwtDecoder;

        // ---------------------------------------------------------------------------
        // GET /api/boats
        // ---------------------------------------------------------------------------

        @Nested
        @DisplayName("GET /api/boats — list with pagination")
        class GetAllBoats {

                @Test
                @WithMockUser(username = "testuser")
                @DisplayName("should return 200 with page response when authenticated")
                void should_return200WithPageResponse_when_authenticated() throws Exception {
                        // Arrange
                        BoatDto.PageResponse pageResponse = BoatDto.PageResponse.builder()
                                        .content(List.of(
                                                        BoatDto.Response.builder().id(1L).name("Sea Explorer").build()))
                                        .page(0).size(10).totalElements(1).totalPages(1).last(true)
                                        .build();

                        when(boatService.findAll(any(), anyInt(), anyInt(), any(), any())).thenReturn(pageResponse);

                        // Act & Assert
                        mockMvc.perform(get("/api/boats").with(jwt()))
                                        .andExpect(status().isOk())
                                        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                                        .andExpect(jsonPath("$.content").isArray())
                                        .andExpect(jsonPath("$.content[0].name").value("Sea Explorer"))
                                        .andExpect(jsonPath("$.totalElements").value(1))
                                        .andExpect(jsonPath("$.last").value(true));
                }

                @Test
                @DisplayName("should return 401 when request is unauthenticated")
                void should_return401_when_unauthenticated() throws Exception {
                        // Act & Assert — No @WithMockUser → no authentication → 401
                        mockMvc.perform(get("/api/boats"))
                                        .andExpect(status().isUnauthorized());

                        verifyNoInteractions(boatService);
                }

                @Test
                @WithMockUser(username = "testuser")
                @DisplayName("should forward all query parameters to the service")
                void should_forwardQueryParamsToService_when_paramsAreProvided() throws Exception {
                        // Arrange
                        when(boatService.findAll("explorer", 2, 5, "name", "asc"))
                                        .thenReturn(BoatDto.PageResponse.builder()
                                                        .content(List.of()).page(2).size(5).totalElements(0)
                                                        .totalPages(0).last(true).build());

                        // Act
                        mockMvc.perform(get("/api/boats").with(jwt())
                                        .param("search", "explorer")
                                        .param("page", "2")
                                        .param("size", "5")
                                        .param("sortBy", "name")
                                        .param("sortDir", "asc"))
                                        .andExpect(status().isOk());

                        // Assert — service receives exactly what the controller received
                        verify(boatService).findAll("explorer", 2, 5, "name", "asc");
                }

                @Test
                @WithMockUser(username = "testuser")
                @DisplayName("should use default values (search='', page=0, size=10, sortBy=createdAt, sortDir=desc)")
                void should_useDefaultQueryParams_when_noneProvided() throws Exception {
                        // Arrange
                        when(boatService.findAll("", 0, 10, "createdAt", "desc"))
                                        .thenReturn(BoatDto.PageResponse.builder()
                                                        .content(List.of()).page(0).size(10).totalElements(0)
                                                        .totalPages(0).last(true).build());

                        // Act & Assert
                        mockMvc.perform(get("/api/boats").with(jwt()))
                                        .andExpect(status().isOk());

                        verify(boatService).findAll("", 0, 10, "createdAt", "desc");
                }
        }

        // ---------------------------------------------------------------------------
        // GET /api/boats/{id}
        // ---------------------------------------------------------------------------

        @Nested
        @DisplayName("GET /api/boats/{id} — get single boat")
        class GetBoatById {

                @Test
                @WithMockUser(username = "testuser")
                @DisplayName("should return 200 with boat details when boat exists")
                void should_return200WithBoatDetails_when_boatExists() throws Exception {
                        // Arrange
                        BoatDto.Response response = BoatDto.Response.builder()
                                        .id(1L).name("Sea Explorer").description("A sailing boat")
                                        .createdAt(Instant.now()).build();

                        when(boatService.findById(1L)).thenReturn(response);

                        // Act & Assert
                        mockMvc.perform(get("/api/boats/1").with(jwt()))
                                        .andExpect(status().isOk())
                                        .andExpect(jsonPath("$.id").value(1))
                                        .andExpect(jsonPath("$.name").value("Sea Explorer"))
                                        .andExpect(jsonPath("$.description").value("A sailing boat"))
                                        .andExpect(jsonPath("$.createdAt").isNotEmpty());
                }

                @Test
                @WithMockUser(username = "testuser")
                @DisplayName("should return 404 when boat does not exist")
                void should_return404_when_boatDoesNotExist() throws Exception {
                        // Arrange
                        when(boatService.findById(99L)).thenThrow(new BoatNotFoundException(99L));

                        // Act & Assert
                        mockMvc.perform(get("/api/boats/99").with(jwt()))
                                        .andExpect(status().isNotFound())
                                        .andExpect(jsonPath("$.title").value("Boat Not Found"));
                }

                @Test
                @DisplayName("should return 401 when request is unauthenticated")
                void should_return401_when_unauthenticated() throws Exception {
                        // Act & Assert
                        mockMvc.perform(get("/api/boats/1"))
                                        .andExpect(status().isUnauthorized());

                        verifyNoInteractions(boatService);
                }
        }

        // ---------------------------------------------------------------------------
        // POST /api/boats
        // ---------------------------------------------------------------------------

        @Nested
        @DisplayName("POST /api/boats — create boat")
        class CreateBoat {

                @Test
                @WithMockUser(username = "testuser")
                @DisplayName("should return 201 with created boat when request is valid")
                void should_return201WithCreatedBoat_when_requestIsValid() throws Exception {
                        // Arrange
                        BoatDto.Request request = new BoatDto.Request("New Boat", "A new boat");
                        BoatDto.Response response = BoatDto.Response.builder()
                                        .id(1L).name("New Boat").description("A new boat").createdAt(Instant.now())
                                        .build();

                        when(boatService.create(any(BoatDto.Request.class))).thenReturn(response);

                        // Act & Assert
                        mockMvc.perform(post("/api/boats").with(jwt())
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(objectMapper.writeValueAsString(request)))
                                        .andExpect(status().isCreated())
                                        .andExpect(jsonPath("$.id").value(1))
                                        .andExpect(jsonPath("$.name").value("New Boat"))
                                        .andExpect(jsonPath("$.description").value("A new boat"));
                }

                @Test
                @WithMockUser(username = "testuser")
                @DisplayName("should return 201 when description is absent (optional field)")
                void should_return201_when_descriptionIsAbsent() throws Exception {
                        // Arrange — description is not required
                        BoatDto.Request request = new BoatDto.Request("Minimal Boat", null);
                        BoatDto.Response response = BoatDto.Response.builder()
                                        .id(2L).name("Minimal Boat").description(null).createdAt(Instant.now()).build();

                        when(boatService.create(any(BoatDto.Request.class))).thenReturn(response);

                        // Act & Assert
                        mockMvc.perform(post("/api/boats").with(jwt())
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(objectMapper.writeValueAsString(request)))
                                        .andExpect(status().isCreated())
                                        .andExpect(jsonPath("$.id").value(2));
                }

                @Test
                @WithMockUser(username = "testuser")
                @DisplayName("should return 400 when name is blank (empty string)")
                void should_return400_when_nameIsBlank() throws Exception {
                        // Arrange — empty string fails @NotBlank
                        BoatDto.Request request = new BoatDto.Request("", "Some description");

                        // Act & Assert
                        mockMvc.perform(post("/api/boats").with(jwt())
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(objectMapper.writeValueAsString(request)))
                                        .andExpect(status().isBadRequest())
                                        .andExpect(jsonPath("$.title").value("Validation Error"));

                        verifyNoInteractions(boatService);
                }

                @Test
                @WithMockUser(username = "testuser")
                @DisplayName("should return 400 when name is whitespace only (e.g. '   ')")
                void should_return400_when_nameIsWhitespaceOnly() throws Exception {
                        // Arrange — whitespace-only also fails @NotBlank
                        BoatDto.Request request = new BoatDto.Request("   ", "Some description");

                        // Act & Assert
                        mockMvc.perform(post("/api/boats").with(jwt())
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(objectMapper.writeValueAsString(request)))
                                        .andExpect(status().isBadRequest());

                        verifyNoInteractions(boatService);
                }

                @Test
                @WithMockUser(username = "testuser")
                @DisplayName("should return 400 when name is missing from the JSON body")
                void should_return400_when_nameIsAbsent() throws Exception {
                        // Arrange — JSON with no "name" key
                        String requestJson = "{\"description\": \"Some description\"}";

                        // Act & Assert
                        mockMvc.perform(post("/api/boats").with(jwt())
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(requestJson))
                                        .andExpect(status().isBadRequest());

                        verifyNoInteractions(boatService);
                }

                @Test
                @WithMockUser(username = "testuser")
                @DisplayName("should return 400 when description exceeds 2000 characters")
                void should_return400_when_descriptionExceedsMaxLength() throws Exception {
                        // Arrange — 2001 chars exceeds the @Size(max=2000) constraint
                        BoatDto.Request request = new BoatDto.Request("Valid Name", "x".repeat(2001));

                        // Act & Assert
                        mockMvc.perform(post("/api/boats").with(jwt())
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(objectMapper.writeValueAsString(request)))
                                        .andExpect(status().isBadRequest());

                        verifyNoInteractions(boatService);
                }

                @Test
                @WithMockUser(username = "testuser")
                @DisplayName("should return 201 when name is exactly 1 character (lower boundary)")
                void should_return201_when_nameIsOneCharacter() throws Exception {
                        // Arrange — 1-char name is valid per @Size(min=1, max=255)
                        BoatDto.Request request = new BoatDto.Request("A", null);
                        BoatDto.Response response = BoatDto.Response.builder()
                                        .id(1L).name("A").createdAt(Instant.now()).build();

                        when(boatService.create(any())).thenReturn(response);

                        // Act & Assert
                        mockMvc.perform(post("/api/boats").with(jwt())
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(objectMapper.writeValueAsString(request)))
                                        .andExpect(status().isCreated());
                }

                @Test
                @WithMockUser(username = "testuser")
                @DisplayName("should return 201 when name is exactly 255 characters (upper boundary)")
                void should_return201_when_nameIs255Characters() throws Exception {
                        // Arrange — 255-char name is valid per @Size(max=255)
                        String maxName = "A".repeat(255);
                        BoatDto.Request request = new BoatDto.Request(maxName, null);
                        BoatDto.Response response = BoatDto.Response.builder()
                                        .id(1L).name(maxName).createdAt(Instant.now()).build();

                        when(boatService.create(any())).thenReturn(response);

                        // Act & Assert
                        mockMvc.perform(post("/api/boats").with(jwt())
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(objectMapper.writeValueAsString(request)))
                                        .andExpect(status().isCreated());
                }

                @Test
                @WithMockUser(username = "testuser")
                @DisplayName("should return 400 when name exceeds 255 characters")
                void should_return400_when_nameExceedsMaxLength() throws Exception {
                        // Arrange — 256 chars violates @Size(max=255)
                        BoatDto.Request request = new BoatDto.Request("A".repeat(256), null);

                        // Act & Assert
                        mockMvc.perform(post("/api/boats").with(jwt())
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(objectMapper.writeValueAsString(request)))
                                        .andExpect(status().isBadRequest());

                        verifyNoInteractions(boatService);
                }

                @Test
                @WithMockUser(username = "testuser")
                @DisplayName("should return 400 when the body is malformed JSON")
                void should_return400_when_bodyIsMalformedJson() throws Exception {
                        // Arrange — syntactically invalid JSON
                        String malformed = "{ name: 'not valid json' }";

                        // Act & Assert
                        mockMvc.perform(post("/api/boats").with(jwt())
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(malformed))
                                        .andExpect(status().isBadRequest());

                        verifyNoInteractions(boatService);
                }

                @Test
                @DisplayName("should return 403 when request is unauthenticated")
                void should_return403_when_unauthenticated() throws Exception {
                        // Act & Assert
                        mockMvc.perform(post("/api/boats")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(objectMapper.writeValueAsString(new BoatDto.Request("Boat", null))))
                                        .andExpect(status().isForbidden());
                }
        }

        // ---------------------------------------------------------------------------
        // PUT /api/boats/{id}
        // ---------------------------------------------------------------------------

        @Nested
        @DisplayName("PUT /api/boats/{id} — update boat")
        class UpdateBoat {

                @Test
                @WithMockUser(username = "testuser")
                @DisplayName("should return 200 with updated boat when request is valid and boat exists")
                void should_return200WithUpdatedBoat_when_requestIsValidAndBoatExists() throws Exception {
                        // Arrange
                        BoatDto.Request request = new BoatDto.Request("Updated Name", "Updated description");
                        BoatDto.Response response = BoatDto.Response.builder()
                                        .id(1L).name("Updated Name").description("Updated description")
                                        .createdAt(Instant.now()).build();

                        when(boatService.update(eq(1L), any(BoatDto.Request.class))).thenReturn(response);

                        // Act & Assert
                        mockMvc.perform(put("/api/boats/1").with(jwt())
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(objectMapper.writeValueAsString(request)))
                                        .andExpect(status().isOk())
                                        .andExpect(jsonPath("$.id").value(1))
                                        .andExpect(jsonPath("$.name").value("Updated Name"))
                                        .andExpect(jsonPath("$.description").value("Updated description"));
                }

                @Test
                @WithMockUser(username = "testuser")
                @DisplayName("should return 404 when boat does not exist")
                void should_return404_when_boatDoesNotExist() throws Exception {
                        // Arrange
                        when(boatService.update(eq(99L), any())).thenThrow(new BoatNotFoundException(99L));

                        // Act & Assert
                        mockMvc.perform(put("/api/boats/99").with(jwt())
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(objectMapper.writeValueAsString(new BoatDto.Request("Name", "Desc"))))
                                        .andExpect(status().isNotFound());
                }

                @Test
                @WithMockUser(username = "testuser")
                @DisplayName("should return 400 when name is blank on update")
                void should_return400_when_nameIsBlankOnUpdate() throws Exception {
                        // Arrange — same validation rules apply for update
                        BoatDto.Request request = new BoatDto.Request("", "Description");

                        // Act & Assert
                        mockMvc.perform(put("/api/boats/1").with(jwt())
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(objectMapper.writeValueAsString(request)))
                                        .andExpect(status().isBadRequest());

                        verifyNoInteractions(boatService);
                }

                @Test
                @WithMockUser(username = "testuser")
                @DisplayName("should return 400 when name is whitespace only on update")
                void should_return400_when_nameIsWhitespaceOnUpdate() throws Exception {
                        // Arrange
                        BoatDto.Request request = new BoatDto.Request("   ", "Description");

                        // Act & Assert
                        mockMvc.perform(put("/api/boats/1").with(jwt())
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(objectMapper.writeValueAsString(request)))
                                        .andExpect(status().isBadRequest());

                        verifyNoInteractions(boatService);
                }

                @Test
                @DisplayName("should return 403 when request is unauthenticated")
                void should_return403_when_unauthenticated() throws Exception {
                        // Act & Assert
                        mockMvc.perform(put("/api/boats/1")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(objectMapper.writeValueAsString(new BoatDto.Request("Name", "Desc"))))
                                        .andExpect(status().isForbidden());
                }
        }

        // ---------------------------------------------------------------------------
        // DELETE /api/boats/{id}
        // ---------------------------------------------------------------------------

        @Nested
        @DisplayName("DELETE /api/boats/{id} — delete boat")
        class DeleteBoat {

                @Test
                @WithMockUser(username = "testuser")
                @DisplayName("should return 204 with no body when boat exists")
                void should_return204_when_boatExists() throws Exception {
                        // Arrange
                        doNothing().when(boatService).delete(1L);

                        // Act & Assert
                        mockMvc.perform(delete("/api/boats/1").with(jwt()))
                                        .andExpect(status().isNoContent())
                                        // 204 must have no response body
                                        .andExpect(content().string(""));

                        verify(boatService).delete(1L);
                }

                @Test
                @WithMockUser(username = "testuser")
                @DisplayName("should return 404 when boat does not exist")
                void should_return404_when_boatDoesNotExist() throws Exception {
                        // Arrange
                        doThrow(new BoatNotFoundException(99L)).when(boatService).delete(99L);

                        // Act & Assert
                        mockMvc.perform(delete("/api/boats/99").with(jwt()))
                                        .andExpect(status().isNotFound())
                                        .andExpect(jsonPath("$.title").value("Boat Not Found"));
                }

                @Test
                @DisplayName("should return 403 when request is unauthenticated")
                void should_return403_when_unauthenticated() throws Exception {
                        // Act & Assert
                        mockMvc.perform(delete("/api/boats/1"))
                                        .andExpect(status().isForbidden());

                        verifyNoInteractions(boatService);
                }
        }
}
