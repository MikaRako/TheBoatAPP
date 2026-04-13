package com.boatmanagement;

import com.boatmanagement.dto.BoatDto;
import com.boatmanagement.entity.BoatStatus;
import com.boatmanagement.entity.BoatType;
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
        BoatDto.Request request = BoatDto.Request.builder()
                .name("Test Boat").description("A test boat")
                .status(BoatStatus.IN_PORT).type(BoatType.YACHT).build();

        String responseBody = mockMvc.perform(post("/api/boats")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Test Boat"))
                .andExpect(jsonPath("$.description").value("A test boat"))
                .andExpect(jsonPath("$.status").value("IN_PORT"))
                .andExpect(jsonPath("$.type").value("YACHT"))
                .andReturn().getResponse().getContentAsString();

        BoatDto.Response created = objectMapper.readValue(responseBody, BoatDto.Response.class);

        // Retrieve boat
        mockMvc.perform(get("/api/boats/" + created.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(created.getId()))
                .andExpect(jsonPath("$.name").value("Test Boat"))
                .andExpect(jsonPath("$.status").value("IN_PORT"))
                .andExpect(jsonPath("$.type").value("YACHT"));
    }

    @Test
    @WithMockUser(username = "testuser")
    void shouldReturnValidationErrorWhenNameIsBlank() throws Exception {
        BoatDto.Request request = BoatDto.Request.builder()
                .name("").description("A test boat")
                .status(BoatStatus.IN_PORT).type(BoatType.YACHT).build();

        mockMvc.perform(post("/api/boats")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "testuser")
    void shouldReturnValidationErrorWhenStatusIsAbsent() throws Exception {
        // status is @NotNull — missing it must produce 400
        String requestJson = "{\"name\": \"My Boat\", \"type\": \"YACHT\"}";

        mockMvc.perform(post("/api/boats")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "testuser")
    void shouldReturnValidationErrorWhenTypeIsAbsent() throws Exception {
        // type is @NotNull — missing it must produce 400
        String requestJson = "{\"name\": \"My Boat\", \"status\": \"IN_PORT\"}";

        mockMvc.perform(post("/api/boats")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
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
        BoatDto.Request request = BoatDto.Request.builder()
                .name("Delete Me").description("To be deleted")
                .status(BoatStatus.MAINTENANCE).type(BoatType.TRAWLER).build();

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

    @Test
    @WithMockUser(username = "testuser")
    void shouldFilterBoatsByStatus() throws Exception {
        // Create two boats with different statuses
        BoatDto.Request underwayBoat = BoatDto.Request.builder()
                .name("Underway Vessel").status(BoatStatus.UNDERWAY).type(BoatType.TRAWLER).build();
        BoatDto.Request inPortBoat = BoatDto.Request.builder()
                .name("Docked Vessel").status(BoatStatus.IN_PORT).type(BoatType.YACHT).build();

        mockMvc.perform(post("/api/boats").contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(underwayBoat))).andExpect(status().isCreated());
        mockMvc.perform(post("/api/boats").contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(inPortBoat))).andExpect(status().isCreated());

        // Filter by UNDERWAY — should return only the first one
        mockMvc.perform(get("/api/boats").param("status", "UNDERWAY"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].name").value("Underway Vessel"));
    }

    @Test
    @WithMockUser(username = "testuser")
    void shouldFilterBoatsByType() throws Exception {
        // Create two boats with different types
        BoatDto.Request trawler = BoatDto.Request.builder()
                .name("Fishing Boat").status(BoatStatus.UNDERWAY).type(BoatType.TRAWLER).build();
        BoatDto.Request ferry = BoatDto.Request.builder()
                .name("Passenger Ferry").status(BoatStatus.IN_PORT).type(BoatType.FERRY).build();

        mockMvc.perform(post("/api/boats").contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(trawler))).andExpect(status().isCreated());
        mockMvc.perform(post("/api/boats").contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(ferry))).andExpect(status().isCreated());

        // Filter by FERRY — should return only the second one
        mockMvc.perform(get("/api/boats").param("type", "FERRY"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].name").value("Passenger Ferry"));
    }
}
