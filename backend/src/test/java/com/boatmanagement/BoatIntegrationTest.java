package com.boatmanagement;

import com.boatmanagement.dto.BoatDto;
import com.boatmanagement.repository.BoatRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import com.boatmanagement.test.EnabledIfDockerAvailable;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Testcontainers
@EnabledIfDockerAvailable
class BoatIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("testdb")
            .withUsername("testuser")
            .withPassword("testpassword");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.security.oauth2.resourceserver.jwt.issuer-uri",
                () -> "http://localhost:8080/realms/boat-realm");
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private BoatRepository boatRepository;

    @BeforeEach
    void setUp() {
        boatRepository.deleteAll();
    }

    @Test
    @WithMockUser(username = "testuser")
    void shouldCreateAndRetrieveBoat() throws Exception {
        // Create boat
        BoatDto.Request request = new BoatDto.Request("Test Boat", "A test boat");
        String responseBody = mockMvc.perform(post("/api/boats")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Test Boat"))
                .andExpect(jsonPath("$.description").value("A test boat"))
                .andReturn().getResponse().getContentAsString();

        BoatDto.Response created = objectMapper.readValue(responseBody, BoatDto.Response.class);

        // Retrieve boat
        mockMvc.perform(get("/api/boats/" + created.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(created.getId()))
                .andExpect(jsonPath("$.name").value("Test Boat"));
    }

    @Test
    @WithMockUser(username = "testuser")
    void shouldReturnValidationErrorWhenNameIsBlank() throws Exception {
        BoatDto.Request request = new BoatDto.Request("", "A test boat");
        mockMvc.perform(post("/api/boats")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "testuser")
    void shouldReturn404WhenBoatNotFound() throws Exception {
        mockMvc.perform(get("/api/boats/999999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldReturn401WhenNotAuthenticated() throws Exception {
        mockMvc.perform(get("/api/boats"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "testuser")
    void shouldDeleteBoat() throws Exception {
        BoatDto.Request request = new BoatDto.Request("Delete Me", "To be deleted");
        String responseBody = mockMvc.perform(post("/api/boats")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        BoatDto.Response created = objectMapper.readValue(responseBody, BoatDto.Response.class);

        mockMvc.perform(delete("/api/boats/" + created.getId()))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/boats/" + created.getId()))
                .andExpect(status().isNotFound());
    }
}
