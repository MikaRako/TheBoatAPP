package com.boatmanagement.repository;

import com.boatmanagement.entity.Boat;
import com.boatmanagement.entity.BoatStatus;
import com.boatmanagement.entity.BoatType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import com.boatmanagement.test.EnabledIfDockerAvailable;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Testing strategy — Output Adapter (Persistence):
 *
 * These tests verify the persistence adapter in isolation using a real
 * PostgreSQL database managed by TestContainers. This ensures our JPQL queries,
 * Hibernate mappings, and column constraints behave exactly as they would in
 * production.
 *
 * @DataJpaTest loads only the JPA slice (entities, repositories), making tests
 *              significantly faster than a full @SpringBootTest while still
 *              using real SQL.
 * @AutoConfigureTestDatabase(replace=NONE) prevents Spring from swapping in H2.
 *
 *              Each @Nested class covers one group of repository operations.
 *              A @BeforeEach clears the table so tests are fully isolated.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
@EnabledIfDockerAvailable
@DisplayName("BoatRepository — persistence output adapter tests")
class BoatRepositoryTest {

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
    }

    @Autowired
    private BoatRepository boatRepository;

    @BeforeEach
    void cleanDatabase() {
        boatRepository.deleteAll();
    }

    // ---------------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------------

    private Boat saveBoat(String name, String description) {
        return boatRepository.saveAndFlush(
                Boat.builder().name(name).description(description).build());
    }

    private Boat saveBoat(String name, String description, BoatStatus status, BoatType type) {
        return boatRepository.saveAndFlush(
                Boat.builder().name(name).description(description).status(status).type(type).build());
    }

    // ---------------------------------------------------------------------------
    // Persistence — save & findById
    // ---------------------------------------------------------------------------

    @Nested
    @DisplayName("save() and findById() — persistence guarantees")
    class Persistence {

        @Test
        @DisplayName("should assign a positive auto-generated id on first save")
        void should_assignPositiveId_when_boatIsSavedForTheFirstTime() {
            // Arrange
            Boat boat = Boat.builder().name("Sea Explorer").description("A sailing boat").build();

            // Act
            Boat saved = boatRepository.save(boat);

            // Assert
            assertThat(saved.getId()).isNotNull().isPositive();
        }

        @Test
        @DisplayName("should persist name, description, status, and type faithfully")
        void should_persistAllFields_when_saved() {
            // Arrange & Act
            Boat saved = saveBoat("Sea Explorer", "A luxury sailing boat", BoatStatus.UNDERWAY, BoatType.SAILBOAT);

            // Assert
            Optional<Boat> found = boatRepository.findById(saved.getId());
            assertThat(found).isPresent();
            assertThat(found.get().getName()).isEqualTo("Sea Explorer");
            assertThat(found.get().getDescription()).isEqualTo("A luxury sailing boat");
            assertThat(found.get().getStatus()).isEqualTo(BoatStatus.UNDERWAY);
            assertThat(found.get().getType()).isEqualTo(BoatType.SAILBOAT);
        }

        @Test
        @DisplayName("should apply IN_PORT and YACHT defaults when status and type are not set explicitly")
        void should_applyDefaults_when_statusAndTypeAreNotSet() {
            // Arrange & Act — saveBoat(name, desc) uses the 2-arg helper with no status/type
            Boat saved = saveBoat("Default Boat", "No explicit status or type");

            // Assert
            Optional<Boat> found = boatRepository.findById(saved.getId());
            assertThat(found).isPresent();
            assertThat(found.get().getStatus()).isEqualTo(BoatStatus.IN_PORT);
            assertThat(found.get().getType()).isEqualTo(BoatType.YACHT);
        }

        @Test
        @DisplayName("should populate createdAt automatically (Hibernate @CreationTimestamp)")
        void should_populateCreatedAt_when_saved() {
            // Arrange & Act
            Boat saved = saveBoat("Timestamped Boat", "Has a timestamp");

            // Assert — @CreationTimestamp must set the field upon INSERT
            assertThat(saved.getCreatedAt()).isNotNull();
        }

        @Test
        @DisplayName("should allow a null description (optional field)")
        void should_allowNullDescription_when_saved() {
            // Arrange & Act
            Boat saved = boatRepository.save(Boat.builder().name("No-desc Boat").build());

            // Assert
            assertThat(saved.getId()).isNotNull();
            assertThat(saved.getDescription()).isNull();
        }

        @Test
        @DisplayName("should return empty Optional for a non-existent id")
        void should_returnEmptyOptional_when_idDoesNotExist() {
            // Act & Assert
            assertThat(boatRepository.findById(99999L)).isEmpty();
        }

        @Test
        @DisplayName("should update the entity when saved again with the same id")
        void should_updateExistingEntity_when_savedWithSameId() {
            // Arrange
            Boat saved = saveBoat("Original Name", "Original description");

            // Act — modify and save again (simulates service.update)
            saved.setName("Updated Name");
            saved.setDescription("Updated description");
            saved.setStatus(BoatStatus.MAINTENANCE);
            saved.setType(BoatType.FERRY);
            Boat updated = boatRepository.save(saved);

            // Assert
            assertThat(updated.getName()).isEqualTo("Updated Name");
            assertThat(updated.getDescription()).isEqualTo("Updated description");
            assertThat(updated.getStatus()).isEqualTo(BoatStatus.MAINTENANCE);
            assertThat(updated.getType()).isEqualTo(BoatType.FERRY);
            assertThat(boatRepository.count()).isEqualTo(1); // no duplicate created
        }
    }

    // ---------------------------------------------------------------------------
    // existsById / deleteById
    // ---------------------------------------------------------------------------

    @Nested
    @DisplayName("existsById() and deleteById() — existence and deletion")
    class ExistenceAndDeletion {

        @Test
        @DisplayName("should return true when boat exists")
        void should_returnTrue_when_boatExists() {
            // Arrange
            Boat saved = saveBoat("Exists", "This boat exists");

            // Act & Assert
            assertThat(boatRepository.existsById(saved.getId())).isTrue();
        }

        @Test
        @DisplayName("should return false when boat does not exist")
        void should_returnFalse_when_boatDoesNotExist() {
            // Act & Assert
            assertThat(boatRepository.existsById(99999L)).isFalse();
        }

        @Test
        @DisplayName("should remove the boat from the database on deleteById")
        void should_removeBoat_when_deletedById() {
            // Arrange
            Boat saved = saveBoat("To Delete", "Will be deleted");

            // Act
            boatRepository.deleteById(saved.getId());

            // Assert
            assertThat(boatRepository.findById(saved.getId())).isEmpty();
            assertThat(boatRepository.count()).isEqualTo(0);
        }

        @Test
        @DisplayName("should not affect other rows when one boat is deleted")
        void should_notAffectOtherBoats_when_oneBoatIsDeleted() {
            // Arrange
            Boat toDelete = saveBoat("Delete Me", "Gone");
            Boat toKeep = saveBoat("Keep Me", "Stays");

            // Act
            boatRepository.deleteById(toDelete.getId());

            // Assert
            assertThat(boatRepository.count()).isEqualTo(1);
            assertThat(boatRepository.findById(toKeep.getId())).isPresent();
        }
    }

    // ---------------------------------------------------------------------------
    // findByFilters — custom JPQL query
    // ---------------------------------------------------------------------------

    @Nested
    @DisplayName("findByFilters() — custom JPQL search and filter query")
    class FindByFilters {

        // --- no filter ---

        @Test
        @DisplayName("should return all boats when search term is empty and no filters are set")
        void should_returnAllBoats_when_searchTermIsEmptyAndNoFilters() {
            // Arrange
            saveBoat("Boat One", "First");
            saveBoat("Boat Two", "Second");

            // Act
            Page<Boat> result = boatRepository.findByFilters("", null, null, PageRequest.of(0, 10));

            // Assert
            assertThat(result.getTotalElements()).isEqualTo(2);
        }

        @Test
        @DisplayName("should return all boats when search term is null")
        void should_returnAllBoats_when_searchTermIsNull() {
            // Arrange
            saveBoat("Boat One", "First");

            // Act
            Page<Boat> result = boatRepository.findByFilters(null, null, null, PageRequest.of(0, 10));

            // Assert
            assertThat(result.getTotalElements()).isEqualTo(1);
        }

        // --- match by name ---

        @Test
        @DisplayName("should return only matching boats when search term matches name exactly")
        void should_returnMatchingBoat_when_searchMatchesNameExactly() {
            // Arrange
            saveBoat("Sea Explorer", "A sailing boat");
            saveBoat("River Runner", "A river boat");

            // Act
            Page<Boat> result = boatRepository.findByFilters("Sea Explorer", null, null, PageRequest.of(0, 10));

            // Assert
            assertThat(result.getTotalElements()).isEqualTo(1);
            assertThat(result.getContent().get(0).getName()).isEqualTo("Sea Explorer");
        }

        @Test
        @DisplayName("should return matching boats when search term is a partial name match")
        void should_returnMatchingBoats_when_searchIsPartialNameMatch() {
            // Arrange
            saveBoat("Sea Explorer", "A sailing boat");
            saveBoat("Sea Breeze", "A fast boat");
            saveBoat("River Runner", "A river boat");

            // Act
            Page<Boat> result = boatRepository.findByFilters("Sea", null, null, PageRequest.of(0, 10));

            // Assert
            assertThat(result.getTotalElements()).isEqualTo(2);
            assertThat(result.getContent()).extracting(Boat::getName)
                    .containsExactlyInAnyOrder("Sea Explorer", "Sea Breeze");
        }

        // --- match by description ---

        @Test
        @DisplayName("should return matching boat when search term matches description")
        void should_returnMatchingBoat_when_searchMatchesDescription() {
            // Arrange
            saveBoat("Boat One", "A luxury yacht");
            saveBoat("Boat Two", "A simple dinghy");

            // Act
            Page<Boat> result = boatRepository.findByFilters("luxury", null, null, PageRequest.of(0, 10));

            // Assert
            assertThat(result.getTotalElements()).isEqualTo(1);
            assertThat(result.getContent().get(0).getName()).isEqualTo("Boat One");
        }

        @Test
        @DisplayName("should return matching boat when search term matches a partial description")
        void should_returnMatchingBoat_when_searchIsPartialDescriptionMatch() {
            // Arrange
            saveBoat("Fancy Vessel", "This is a grand touring yacht");
            saveBoat("Simple Boat", "Basic transportation");

            // Act
            Page<Boat> result = boatRepository.findByFilters("touring", null, null, PageRequest.of(0, 10));

            // Assert
            assertThat(result.getTotalElements()).isEqualTo(1);
            assertThat(result.getContent().get(0).getName()).isEqualTo("Fancy Vessel");
        }

        // --- case insensitivity ---

        @Test
        @DisplayName("should match regardless of case in the name (case-insensitive)")
        void should_matchName_caseInsensitively() {
            // Arrange
            saveBoat("SEA EXPLORER", "A boat");

            // Act — search with all lowercase
            Page<Boat> result = boatRepository.findByFilters("sea explorer", null, null, PageRequest.of(0, 10));

            // Assert
            assertThat(result.getTotalElements()).isEqualTo(1);
        }

        @Test
        @DisplayName("should match regardless of case in the description (case-insensitive)")
        void should_matchDescription_caseInsensitively() {
            // Arrange
            saveBoat("My Boat", "LUXURY YACHT");

            // Act
            Page<Boat> result = boatRepository.findByFilters("luxury yacht", null, null, PageRequest.of(0, 10));

            // Assert
            assertThat(result.getTotalElements()).isEqualTo(1);
        }

        // --- no match ---

        @Test
        @DisplayName("should return an empty page when no boat matches the search term")
        void should_returnEmptyPage_when_noBoatMatchesSearchTerm() {
            // Arrange
            saveBoat("Sea Explorer", "A sailing boat");

            // Act
            Page<Boat> result = boatRepository.findByFilters("nonexistent", null, null, PageRequest.of(0, 10));

            // Assert
            assertThat(result.getTotalElements()).isEqualTo(0);
            assertThat(result.getContent()).isEmpty();
        }

        // --- no duplicate when both name and description match ---

        @Test
        @DisplayName("should not return duplicates when search term matches both name and description")
        void should_notReturnDuplicates_when_searchMatchesBothNameAndDescription() {
            // Arrange — "boat" appears in both name and description
            saveBoat("Boat One", "A nice boat");
            saveBoat("Sea Explorer", "Luxury vessel");

            // Act
            Page<Boat> result = boatRepository.findByFilters("boat", null, null, PageRequest.of(0, 10));

            // Assert — JPQL OR condition must not duplicate the row
            assertThat(result.getTotalElements()).isEqualTo(1);
        }

        // --- status filter ---

        @Test
        @DisplayName("should return only UNDERWAY boats when filtering by UNDERWAY status")
        void should_returnOnlyUnderwayBoats_when_filterByUnderwayStatus() {
            // Arrange
            saveBoat("Active Vessel", "Out at sea", BoatStatus.UNDERWAY, BoatType.TRAWLER);
            saveBoat("Docked Vessel", "At the pier", BoatStatus.IN_PORT, BoatType.YACHT);
            saveBoat("Broken Vessel", "In the shop", BoatStatus.MAINTENANCE, BoatType.FERRY);

            // Act
            Page<Boat> result = boatRepository.findByFilters("", BoatStatus.UNDERWAY, null, PageRequest.of(0, 10));

            // Assert
            assertThat(result.getTotalElements()).isEqualTo(1);
            assertThat(result.getContent().get(0).getName()).isEqualTo("Active Vessel");
            assertThat(result.getContent().get(0).getStatus()).isEqualTo(BoatStatus.UNDERWAY);
        }

        @Test
        @DisplayName("should return only IN_PORT boats when filtering by IN_PORT status")
        void should_returnOnlyInPortBoats_when_filterByInPortStatus() {
            // Arrange
            saveBoat("Active Vessel", "Out at sea", BoatStatus.UNDERWAY, BoatType.TRAWLER);
            saveBoat("Docked Vessel", "At the pier", BoatStatus.IN_PORT, BoatType.YACHT);
            saveBoat("Second Dock", "Also docked", BoatStatus.IN_PORT, BoatType.SAILBOAT);

            // Act
            Page<Boat> result = boatRepository.findByFilters("", BoatStatus.IN_PORT, null, PageRequest.of(0, 10));

            // Assert
            assertThat(result.getTotalElements()).isEqualTo(2);
            assertThat(result.getContent()).extracting(Boat::getStatus)
                    .containsOnly(BoatStatus.IN_PORT);
        }

        @Test
        @DisplayName("should return only MAINTENANCE boats when filtering by MAINTENANCE status")
        void should_returnOnlyMaintenanceBoats_when_filterByMaintenanceStatus() {
            // Arrange
            saveBoat("Active Vessel", "Out at sea", BoatStatus.UNDERWAY, BoatType.TRAWLER);
            saveBoat("Broken Vessel", "In the shop", BoatStatus.MAINTENANCE, BoatType.CARGO_SHIP);

            // Act
            Page<Boat> result = boatRepository.findByFilters("", BoatStatus.MAINTENANCE, null, PageRequest.of(0, 10));

            // Assert
            assertThat(result.getTotalElements()).isEqualTo(1);
            assertThat(result.getContent().get(0).getStatus()).isEqualTo(BoatStatus.MAINTENANCE);
        }

        @Test
        @DisplayName("should return all boats regardless of status when status filter is null")
        void should_returnAllBoats_when_statusFilterIsNull() {
            // Arrange
            saveBoat("Underway Vessel", "At sea", BoatStatus.UNDERWAY, BoatType.TRAWLER);
            saveBoat("Docked Vessel", "At pier", BoatStatus.IN_PORT, BoatType.YACHT);
            saveBoat("Maintenance Vessel", "In shop", BoatStatus.MAINTENANCE, BoatType.FERRY);

            // Act — null status means no filter
            Page<Boat> result = boatRepository.findByFilters("", null, null, PageRequest.of(0, 10));

            // Assert
            assertThat(result.getTotalElements()).isEqualTo(3);
        }

        // --- type filter ---

        @Test
        @DisplayName("should return only SAILBOAT vessels when filtering by SAILBOAT type")
        void should_returnOnlySailboats_when_filterBySailboatType() {
            // Arrange
            saveBoat("Sailboat One", "A sloop", BoatStatus.IN_PORT, BoatType.SAILBOAT);
            saveBoat("Trawler One", "A fishing boat", BoatStatus.UNDERWAY, BoatType.TRAWLER);
            saveBoat("Sailboat Two", "A ketch", BoatStatus.UNDERWAY, BoatType.SAILBOAT);

            // Act
            Page<Boat> result = boatRepository.findByFilters("", null, BoatType.SAILBOAT, PageRequest.of(0, 10));

            // Assert
            assertThat(result.getTotalElements()).isEqualTo(2);
            assertThat(result.getContent()).extracting(Boat::getType)
                    .containsOnly(BoatType.SAILBOAT);
        }

        @Test
        @DisplayName("should return all boats regardless of type when type filter is null")
        void should_returnAllBoats_when_typeFilterIsNull() {
            // Arrange
            saveBoat("Sailboat", "A sloop", BoatStatus.IN_PORT, BoatType.SAILBOAT);
            saveBoat("Trawler", "A fishing boat", BoatStatus.UNDERWAY, BoatType.TRAWLER);
            saveBoat("Cargo Ship", "A freighter", BoatStatus.UNDERWAY, BoatType.CARGO_SHIP);

            // Act — null type means no filter
            Page<Boat> result = boatRepository.findByFilters("", null, null, PageRequest.of(0, 10));

            // Assert
            assertThat(result.getTotalElements()).isEqualTo(3);
        }

        // --- combined status + type filter ---

        @Test
        @DisplayName("should apply both status and type filters when both are provided")
        void should_returnOnlyMatchingBoats_when_filterByBothStatusAndType() {
            // Arrange
            saveBoat("Underway Trawler", "Fishing", BoatStatus.UNDERWAY, BoatType.TRAWLER);
            saveBoat("Docked Trawler", "At pier", BoatStatus.IN_PORT, BoatType.TRAWLER);
            saveBoat("Underway Ferry", "In service", BoatStatus.UNDERWAY, BoatType.FERRY);

            // Act — only UNDERWAY TRAWLERs
            Page<Boat> result = boatRepository.findByFilters("", BoatStatus.UNDERWAY, BoatType.TRAWLER, PageRequest.of(0, 10));

            // Assert
            assertThat(result.getTotalElements()).isEqualTo(1);
            assertThat(result.getContent().get(0).getName()).isEqualTo("Underway Trawler");
        }

        @Test
        @DisplayName("should return empty page when no boat matches the combined status and type filter")
        void should_returnEmptyPage_when_noCombinedMatch() {
            // Arrange — no MAINTENANCE YACHTs
            saveBoat("Maintenance Ferry", "In shop", BoatStatus.MAINTENANCE, BoatType.FERRY);
            saveBoat("Docked Yacht", "At pier", BoatStatus.IN_PORT, BoatType.YACHT);

            // Act
            Page<Boat> result = boatRepository.findByFilters("", BoatStatus.MAINTENANCE, BoatType.YACHT, PageRequest.of(0, 10));

            // Assert
            assertThat(result.getTotalElements()).isEqualTo(0);
        }

        // --- search + status filter ---

        @Test
        @DisplayName("should apply search and status filter together")
        void should_returnMatchingBoats_when_searchAndStatusAreCombined() {
            // Arrange
            saveBoat("Sea Trawler", "Fishing vessel", BoatStatus.UNDERWAY, BoatType.TRAWLER);
            saveBoat("Sea Ferry", "Passenger ferry", BoatStatus.IN_PORT, BoatType.FERRY);
            saveBoat("River Ferry", "River service", BoatStatus.IN_PORT, BoatType.FERRY);

            // Act — boats with "Sea" in name AND status = IN_PORT
            Page<Boat> result = boatRepository.findByFilters("Sea", BoatStatus.IN_PORT, null, PageRequest.of(0, 10));

            // Assert — only "Sea Ferry" matches both criteria
            assertThat(result.getTotalElements()).isEqualTo(1);
            assertThat(result.getContent().get(0).getName()).isEqualTo("Sea Ferry");
        }

        // --- pagination ---

        @Test
        @DisplayName("should return first page with correct size when results exceed page size")
        void should_returnFirstPage_when_totalExceedsPageSize() {
            // Arrange — 15 boats, page size 10
            for (int i = 1; i <= 15; i++) {
                saveBoat("Boat " + i, "Description " + i);
            }

            // Act
            Page<Boat> firstPage = boatRepository.findByFilters("", null, null, PageRequest.of(0, 10));

            // Assert
            assertThat(firstPage.getContent()).hasSize(10);
            assertThat(firstPage.getTotalElements()).isEqualTo(15);
            assertThat(firstPage.getTotalPages()).isEqualTo(2);
            assertThat(firstPage.isLast()).isFalse();
        }

        @Test
        @DisplayName("should return last page with remaining elements")
        void should_returnLastPage_with_remainingElements() {
            // Arrange — 15 boats, page size 10
            for (int i = 1; i <= 15; i++) {
                saveBoat("Boat " + i, "Description " + i);
            }

            // Act
            Page<Boat> lastPage = boatRepository.findByFilters("", null, null, PageRequest.of(1, 10));

            // Assert
            assertThat(lastPage.getContent()).hasSize(5);
            assertThat(lastPage.isLast()).isTrue();
        }

        @Test
        @DisplayName("should return an empty page when page index is beyond total pages")
        void should_returnEmptyPage_when_pageIndexExceedsTotalPages() {
            // Arrange
            saveBoat("Only Boat", "Just one");

            // Act — page 5 does not exist
            Page<Boat> result = boatRepository.findByFilters("", null, null, PageRequest.of(5, 10));

            // Assert
            assertThat(result.getContent()).isEmpty();
            assertThat(result.getTotalElements()).isEqualTo(1); // total is still accurate
        }

        // --- sorting ---

        @Test
        @DisplayName("should return boats in ascending alphabetical order when sorted by name ASC")
        void should_returnBoatsInAscendingOrder_when_sortedByNameAsc() {
            // Arrange
            saveBoat("Zebra Boat", "Z");
            saveBoat("Alpha Boat", "A");
            saveBoat("Mike Boat", "M");

            // Act
            Page<Boat> result = boatRepository.findByFilters("", null, null,
                    PageRequest.of(0, 10, Sort.by("name").ascending()));

            // Assert
            assertThat(result.getContent()).extracting(Boat::getName)
                    .containsExactly("Alpha Boat", "Mike Boat", "Zebra Boat");
        }

        @Test
        @DisplayName("should return boats in descending alphabetical order when sorted by name DESC")
        void should_returnBoatsInDescendingOrder_when_sortedByNameDesc() {
            // Arrange
            saveBoat("Zebra Boat", "Z");
            saveBoat("Alpha Boat", "A");
            saveBoat("Mike Boat", "M");

            // Act
            Page<Boat> result = boatRepository.findByFilters("", null, null,
                    PageRequest.of(0, 10, Sort.by("name").descending()));

            // Assert
            assertThat(result.getContent()).extracting(Boat::getName)
                    .containsExactly("Zebra Boat", "Mike Boat", "Alpha Boat");
        }
    }
}
