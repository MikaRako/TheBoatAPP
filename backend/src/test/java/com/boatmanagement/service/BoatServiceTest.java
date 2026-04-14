package com.boatmanagement.service;

import com.boatmanagement.dto.BoatDto;
import com.boatmanagement.entity.Boat;
import com.boatmanagement.entity.BoatStatus;
import com.boatmanagement.entity.BoatType;
import com.boatmanagement.exception.BoatNotFoundException;
import com.boatmanagement.mapper.BoatMapper;
import com.boatmanagement.repository.BoatRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Testing strategy — Application layer (use cases):
 *
 * BoatService is the core of the hexagonal architecture. It sits between the
 * input adapters (REST) and the output ports (repository, mapper). These tests
 * verify each use case in complete isolation:
 *   - BoatRepository is mocked → no database involved
 *   - BoatMapper is mocked    → no MapStruct processor needed
 *
 * Pattern: Arrange / Act / Assert (AAA) throughout.
 * Groups:  One @Nested class per public method of BoatService.
 * Naming:  should_[expectedBehavior]_when_[scenario]
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("BoatService — application layer unit tests")
class BoatServiceTest {

    @Mock
    private BoatRepository boatRepository;

    @Mock
    private BoatMapper boatMapper;

    @InjectMocks
    private BoatService boatService;

    // ---------------------------------------------------------------------------
    // findAll
    // ---------------------------------------------------------------------------

    @Nested
    @DisplayName("findAll() — paginated list use case")
    class FindAll {

        @Test
        @DisplayName("should return page response with mapped content and correct metadata")
        void should_returnPageResponseWithMappedContent_when_boatsExist() {
            // Arrange
            Boat boat = Boat.builder().id(1L).name("Sea Explorer").description("A sailing boat").build();
            Page<Boat> page = new PageImpl<>(List.of(boat), PageRequest.of(0, 10), 1);
            BoatDto.Response responseDto = BoatDto.Response.builder()
                    .id(1L).name("Sea Explorer").description("A sailing boat")
                    .status(BoatStatus.IN_PORT).type(BoatType.YACHT)
                    .createdAt(Instant.now()).build();

            when(boatRepository.findByFilters(eq(""), isNull(), isNull(), any(Pageable.class))).thenReturn(page);
            when(boatMapper.toResponse(boat)).thenReturn(responseDto);

            // Act
            BoatDto.PageResponse result = boatService.findAll("", null, null, 0, 10, "createdAt", "desc");

            // Assert
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).getName()).isEqualTo("Sea Explorer");
            assertThat(result.getTotalElements()).isEqualTo(1);
            assertThat(result.getPage()).isEqualTo(0);
            assertThat(result.getSize()).isEqualTo(10);
            assertThat(result.isLast()).isTrue();
        }

        @Test
        @DisplayName("should return empty page response when no boats exist")
        void should_returnEmptyPageResponse_when_noBoatsExist() {
            // Arrange
            when(boatRepository.findByFilters(any(), any(), any(), any(Pageable.class))).thenReturn(Page.empty());

            // Act
            BoatDto.PageResponse result = boatService.findAll("", null, null, 0, 10, "createdAt", "desc");

            // Assert
            assertThat(result.getContent()).isEmpty();
            assertThat(result.getTotalElements()).isEqualTo(0);
        }

        @Test
        @DisplayName("should forward the search term to the repository")
        void should_passSearchTermToRepository_when_searchIsProvided() {
            // Arrange
            when(boatRepository.findByFilters(eq("explorer"), isNull(), isNull(), any(Pageable.class)))
                    .thenReturn(Page.empty());

            // Act
            boatService.findAll("explorer", null, null, 0, 10, "name", "asc");

            // Assert — repository is called with the exact search term
            verify(boatRepository).findByFilters(eq("explorer"), isNull(), isNull(), any(Pageable.class));
        }

        @Test
        @DisplayName("should forward status filter to the repository when status is provided")
        void should_passStatusFilterToRepository_when_statusIsProvided() {
            // Arrange
            when(boatRepository.findByFilters(eq(""), eq(BoatStatus.UNDERWAY), isNull(), any(Pageable.class)))
                    .thenReturn(Page.empty());

            // Act
            boatService.findAll("", BoatStatus.UNDERWAY, null, 0, 10, "createdAt", "desc");

            // Assert
            verify(boatRepository).findByFilters(eq(""), eq(BoatStatus.UNDERWAY), isNull(), any(Pageable.class));
        }

        @Test
        @DisplayName("should forward type filter to the repository when type is provided")
        void should_passTypeFilterToRepository_when_typeIsProvided() {
            // Arrange
            when(boatRepository.findByFilters(eq(""), isNull(), eq(BoatType.FERRY), any(Pageable.class)))
                    .thenReturn(Page.empty());

            // Act
            boatService.findAll("", null, BoatType.FERRY, 0, 10, "createdAt", "desc");

            // Assert
            verify(boatRepository).findByFilters(eq(""), isNull(), eq(BoatType.FERRY), any(Pageable.class));
        }

        @Test
        @DisplayName("should forward both status and type filters when both are provided")
        void should_passBothFiltersToRepository_when_bothStatusAndTypeAreProvided() {
            // Arrange
            when(boatRepository.findByFilters(eq(""), eq(BoatStatus.MAINTENANCE), eq(BoatType.TRAWLER), any(Pageable.class)))
                    .thenReturn(Page.empty());

            // Act
            boatService.findAll("", BoatStatus.MAINTENANCE, BoatType.TRAWLER, 0, 10, "createdAt", "desc");

            // Assert
            verify(boatRepository).findByFilters(eq(""), eq(BoatStatus.MAINTENANCE), eq(BoatType.TRAWLER), any(Pageable.class));
        }

        @Test
        @DisplayName("should call toResponse mapper for each boat in the page")
        void should_mapEachBoatToResponse_when_pageHasMultipleBoats() {
            // Arrange
            Boat boat1 = Boat.builder().id(1L).name("Boat One").build();
            Boat boat2 = Boat.builder().id(2L).name("Boat Two").build();
            Page<Boat> page = new PageImpl<>(List.of(boat1, boat2));

            when(boatRepository.findByFilters(any(), any(), any(), any())).thenReturn(page);
            when(boatMapper.toResponse(boat1)).thenReturn(
                    BoatDto.Response.builder().id(1L).name("Boat One").build());
            when(boatMapper.toResponse(boat2)).thenReturn(
                    BoatDto.Response.builder().id(2L).name("Boat Two").build());

            // Act
            BoatDto.PageResponse result = boatService.findAll("", null, null, 0, 10, "createdAt", "desc");

            // Assert
            assertThat(result.getContent()).hasSize(2);
            verify(boatMapper).toResponse(boat1);
            verify(boatMapper).toResponse(boat2);
        }

        @Test
        @DisplayName("should apply ascending sort when sortDir is 'asc'")
        void should_applyAscendingSort_when_sortDirIsAsc() {
            // Arrange
            when(boatRepository.findByFilters(any(), any(), any(), any(Pageable.class))).thenReturn(Page.empty());

            // Act
            boatService.findAll("", null, null, 0, 10, "name", "asc");

            // Assert — pageable must carry ASC direction on the requested field
            verify(boatRepository).findByFilters(eq(""), isNull(), isNull(), argThat(pageable ->
                    pageable.getSort().getOrderFor("name") != null &&
                    pageable.getSort().getOrderFor("name").getDirection() == Sort.Direction.ASC
            ));
        }

        @Test
        @DisplayName("should apply descending sort when sortDir is 'desc'")
        void should_applyDescendingSort_when_sortDirIsDesc() {
            // Arrange
            when(boatRepository.findByFilters(any(), any(), any(), any(Pageable.class))).thenReturn(Page.empty());

            // Act
            boatService.findAll("", null, null, 0, 10, "name", "desc");

            // Assert
            verify(boatRepository).findByFilters(eq(""), isNull(), isNull(), argThat(pageable ->
                    pageable.getSort().getOrderFor("name") != null &&
                    pageable.getSort().getOrderFor("name").getDirection() == Sort.Direction.DESC
            ));
        }

        @Test
        @DisplayName("should treat null search as empty string (no filter)")
        void should_treatNullSearchAsEmpty_when_searchIsNull() {
            // Arrange
            when(boatRepository.findByFilters(eq(""), isNull(), isNull(), any(Pageable.class)))
                    .thenReturn(Page.empty());

            // Act — null search must be normalised to "" before hitting the repository
            boatService.findAll(null, null, null, 0, 10, "createdAt", "desc");

            // Assert
            verify(boatRepository).findByFilters(eq(""), isNull(), isNull(), any(Pageable.class));
        }

        @Test
        @DisplayName("should trim whitespace from search term before forwarding to repository")
        void should_trimSearchTerm_when_searchHasLeadingOrTrailingWhitespace() {
            // Arrange
            when(boatRepository.findByFilters(eq("explorer"), isNull(), isNull(), any(Pageable.class)))
                    .thenReturn(Page.empty());

            // Act — whitespace must be stripped
            boatService.findAll("  explorer  ", null, null, 0, 10, "createdAt", "desc");

            // Assert
            verify(boatRepository).findByFilters(eq("explorer"), isNull(), isNull(), any(Pageable.class));
        }

        @Test
        @DisplayName("should fall back to 'createdAt' sort field when sortBy is null")
        void should_fallbackToCreatedAt_when_sortByIsNull() {
            // Arrange
            when(boatRepository.findByFilters(any(), any(), any(), any(Pageable.class))).thenReturn(Page.empty());

            // Act — null sortBy must not cause a NullPointerException
            boatService.findAll("", null, null, 0, 10, null, "desc");

            // Assert — pageable uses the fallback field
            verify(boatRepository).findByFilters(eq(""), isNull(), isNull(), argThat(pageable ->
                    pageable.getSort().getOrderFor("createdAt") != null
            ));
        }

        @Test
        @DisplayName("should fall back to descending sort when sortDir is null")
        void should_fallbackToDescending_when_sortDirIsNull() {
            // Arrange
            when(boatRepository.findByFilters(any(), any(), any(), any(Pageable.class))).thenReturn(Page.empty());

            // Act — null sortDir must not cause a NullPointerException
            boatService.findAll("", null, null, 0, 10, "createdAt", null);

            // Assert — pageable uses DESC as the fallback direction
            verify(boatRepository).findByFilters(eq(""), isNull(), isNull(), argThat(pageable ->
                    pageable.getSort().getOrderFor("createdAt") != null &&
                    pageable.getSort().getOrderFor("createdAt").getDirection() == Sort.Direction.DESC
            ));
        }

        @Test
        @DisplayName("should default to ascending sort for any sortDir value that is not 'desc'")
        void should_defaultToAscendingSort_when_sortDirIsNotDesc() {
            // Arrange — "ASC", "random", "" all produce ascending order
            when(boatRepository.findByFilters(any(), any(), any(), any(Pageable.class))).thenReturn(Page.empty());

            // Act
            boatService.findAll("", null, null, 0, 10, "createdAt", "ASC");

            // Assert
            verify(boatRepository).findByFilters(eq(""), isNull(), isNull(), argThat(pageable ->
                    pageable.getSort().getOrderFor("createdAt") != null &&
                    pageable.getSort().getOrderFor("createdAt").getDirection() == Sort.Direction.ASC
            ));
        }

        @Test
        @DisplayName("should reflect last=false when there are more pages")
        void should_returnLastFalse_when_morePageExist() {
            // Arrange — 25 total elements, page 0, size 10 → not the last page
            Pageable pageable = PageRequest.of(0, 10);
            Page<Boat> page = new PageImpl<>(List.of(), pageable, 25);
            when(boatRepository.findByFilters(any(), any(), any(), any())).thenReturn(page);

            // Act
            BoatDto.PageResponse result = boatService.findAll("", null, null, 0, 10, "createdAt", "desc");

            // Assert
            assertThat(result.isLast()).isFalse();
            assertThat(result.getTotalPages()).isEqualTo(3);
        }
    }

    // ---------------------------------------------------------------------------
    // findById
    // ---------------------------------------------------------------------------

    @Nested
    @DisplayName("findById(Long id) — get single boat use case")
    class FindById {

        @Test
        @DisplayName("should return mapped response when boat exists")
        void should_returnBoatResponse_when_boatExists() {
            // Arrange
            Boat boat = Boat.builder().id(1L).name("Sea Explorer").description("A sailing boat").build();
            BoatDto.Response expected = BoatDto.Response.builder()
                    .id(1L).name("Sea Explorer").description("A sailing boat")
                    .status(BoatStatus.IN_PORT).type(BoatType.YACHT)
                    .createdAt(Instant.now()).build();

            when(boatRepository.findById(1L)).thenReturn(Optional.of(boat));
            when(boatMapper.toResponse(boat)).thenReturn(expected);

            // Act
            BoatDto.Response result = boatService.findById(1L);

            // Assert
            assertThat(result.getId()).isEqualTo(1L);
            assertThat(result.getName()).isEqualTo("Sea Explorer");
            assertThat(result.getDescription()).isEqualTo("A sailing boat");
            assertThat(result.getStatus()).isEqualTo(BoatStatus.IN_PORT);
            assertThat(result.getType()).isEqualTo(BoatType.YACHT);
        }

        @Test
        @DisplayName("should throw BoatNotFoundException when boat does not exist")
        void should_throwBoatNotFoundException_when_boatDoesNotExist() {
            // Arrange
            when(boatRepository.findById(99L)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> boatService.findById(99L))
                    .isInstanceOf(BoatNotFoundException.class)
                    .hasMessageContaining("99");
        }

        @Test
        @DisplayName("should throw BoatNotFoundException for id = 0 (non-existent)")
        void should_throwBoatNotFoundException_when_idIsZero() {
            // Arrange — id=0 is technically valid but will never be persisted by the sequence
            when(boatRepository.findById(0L)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> boatService.findById(0L))
                    .isInstanceOf(BoatNotFoundException.class);
        }

        @Test
        @DisplayName("should never call the mapper when the boat is not found")
        void should_neverCallMapper_when_boatDoesNotExist() {
            // Arrange
            when(boatRepository.findById(any())).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> boatService.findById(1L))
                    .isInstanceOf(BoatNotFoundException.class);

            verifyNoInteractions(boatMapper);
        }
    }

    // ---------------------------------------------------------------------------
    // create
    // ---------------------------------------------------------------------------

    @Nested
    @DisplayName("create(Request) — create boat use case")
    class Create {

        @Test
        @DisplayName("should persist, map, and return the created boat response")
        void should_saveAndReturnResponse_when_requestIsValid() {
            // Arrange
            BoatDto.Request request = BoatDto.Request.builder()
                    .name("New Boat").description("A new boat")
                    .status(BoatStatus.IN_PORT).type(BoatType.YACHT).build();
            Boat unsaved = Boat.builder().name("New Boat").description("A new boat").build();
            Boat saved   = Boat.builder().id(1L).name("New Boat").description("A new boat").build();
            BoatDto.Response expected = BoatDto.Response.builder()
                    .id(1L).name("New Boat").description("A new boat")
                    .status(BoatStatus.IN_PORT).type(BoatType.YACHT)
                    .createdAt(Instant.now()).build();

            when(boatMapper.toEntity(request)).thenReturn(unsaved);
            when(boatRepository.save(unsaved)).thenReturn(saved);
            when(boatMapper.toResponse(saved)).thenReturn(expected);

            // Act
            BoatDto.Response result = boatService.create(request);

            // Assert
            assertThat(result.getId()).isEqualTo(1L);
            assertThat(result.getName()).isEqualTo("New Boat");
            assertThat(result.getStatus()).isEqualTo(BoatStatus.IN_PORT);
            assertThat(result.getType()).isEqualTo(BoatType.YACHT);
            verify(boatMapper).toEntity(request);
            verify(boatRepository).save(unsaved);
            verify(boatMapper).toResponse(saved);
        }

        @Test
        @DisplayName("should persist a boat without a description (optional field)")
        void should_saveBoatWithoutDescription_when_descriptionIsNull() {
            // Arrange — description is optional per the domain model
            BoatDto.Request request = BoatDto.Request.builder()
                    .name("Boat Without Desc")
                    .status(BoatStatus.UNDERWAY).type(BoatType.SAILBOAT).build();
            Boat unsaved = Boat.builder().name("Boat Without Desc").build();
            Boat saved   = Boat.builder().id(2L).name("Boat Without Desc").build();
            BoatDto.Response expected = BoatDto.Response.builder()
                    .id(2L).name("Boat Without Desc")
                    .status(BoatStatus.UNDERWAY).type(BoatType.SAILBOAT)
                    .createdAt(Instant.now()).build();

            when(boatMapper.toEntity(request)).thenReturn(unsaved);
            when(boatRepository.save(unsaved)).thenReturn(saved);
            when(boatMapper.toResponse(saved)).thenReturn(expected);

            // Act
            BoatDto.Response result = boatService.create(request);

            // Assert
            assertThat(result.getDescription()).isNull();
        }
    }

    // ---------------------------------------------------------------------------
    // update
    // ---------------------------------------------------------------------------

    @Nested
    @DisplayName("update(Long id, Request) — update boat use case")
    class Update {

        @Test
        @DisplayName("should update fields and return the updated response")
        void should_updateAndReturnResponse_when_boatExists() {
            // Arrange
            BoatDto.Request request = BoatDto.Request.builder()
                    .name("Updated Name").description("Updated description")
                    .status(BoatStatus.UNDERWAY).type(BoatType.CARGO_SHIP).build();
            Boat existing = Boat.builder().id(1L).name("Old Name").description("Old description").build();
            Boat saved    = Boat.builder().id(1L).name("Updated Name").description("Updated description").build();
            BoatDto.Response expected = BoatDto.Response.builder()
                    .id(1L).name("Updated Name").description("Updated description")
                    .status(BoatStatus.UNDERWAY).type(BoatType.CARGO_SHIP)
                    .createdAt(Instant.now()).build();

            when(boatRepository.findById(1L)).thenReturn(Optional.of(existing));
            when(boatRepository.save(existing)).thenReturn(saved);
            when(boatMapper.toResponse(saved)).thenReturn(expected);

            // Act
            BoatDto.Response result = boatService.update(1L, request);

            // Assert
            assertThat(result.getName()).isEqualTo("Updated Name");
            assertThat(result.getDescription()).isEqualTo("Updated description");
            assertThat(result.getStatus()).isEqualTo(BoatStatus.UNDERWAY);
            assertThat(result.getType()).isEqualTo(BoatType.CARGO_SHIP);

            // Verify that updateEntity was called to mutate the existing entity in-place
            verify(boatMapper).updateEntity(existing, request);
            verify(boatRepository).save(existing);
        }

        @Test
        @DisplayName("should throw BoatNotFoundException without saving when boat does not exist")
        void should_throwBoatNotFoundException_when_boatDoesNotExist() {
            // Arrange
            when(boatRepository.findById(99L)).thenReturn(Optional.empty());

            BoatDto.Request request = BoatDto.Request.builder()
                    .name("Name").description("Desc")
                    .status(BoatStatus.IN_PORT).type(BoatType.YACHT).build();

            // Act & Assert
            assertThatThrownBy(() -> boatService.update(99L, request))
                    .isInstanceOf(BoatNotFoundException.class)
                    .hasMessageContaining("99");

            // Saving must never happen when the entity is not found
            verify(boatRepository, never()).save(any());
        }

        @Test
        @DisplayName("should never call mapper when boat is not found")
        void should_neverCallMapper_when_boatDoesNotExist() {
            // Arrange
            when(boatRepository.findById(any())).thenReturn(Optional.empty());

            BoatDto.Request request = BoatDto.Request.builder()
                    .name("Name").description("Desc")
                    .status(BoatStatus.IN_PORT).type(BoatType.YACHT).build();

            // Act & Assert
            assertThatThrownBy(() -> boatService.update(1L, request))
                    .isInstanceOf(BoatNotFoundException.class);

            verifyNoInteractions(boatMapper);
        }
    }

    // ---------------------------------------------------------------------------
    // delete
    // ---------------------------------------------------------------------------

    @Nested
    @DisplayName("delete(Long id) — delete boat use case")
    class Delete {

        @Test
        @DisplayName("should delete the boat when it exists")
        void should_deleteBoat_when_boatExists() {
            // Arrange
            when(boatRepository.existsById(1L)).thenReturn(true);

            // Act
            boatService.delete(1L);

            // Assert
            verify(boatRepository).deleteById(1L);
        }

        @Test
        @DisplayName("should throw BoatNotFoundException without deleting when boat does not exist")
        void should_throwBoatNotFoundException_when_boatDoesNotExist() {
            // Arrange
            when(boatRepository.existsById(99L)).thenReturn(false);

            // Act & Assert
            assertThatThrownBy(() -> boatService.delete(99L))
                    .isInstanceOf(BoatNotFoundException.class)
                    .hasMessageContaining("99");

            // deleteById must never be called if the entity was not found
            verify(boatRepository, never()).deleteById(any());
        }

        @Test
        @DisplayName("should only check existence once and not load the full entity")
        void should_useExistsById_not_findById_for_existence_check() {
            // Arrange — Using existsById is more efficient than loading the full entity
            when(boatRepository.existsById(1L)).thenReturn(true);

            // Act
            boatService.delete(1L);

            // Assert — findById should never be called in the delete path
            verify(boatRepository, never()).findById(any());
        }
    }
}
