package com.boatmanagement.service;

import com.boatmanagement.dto.BoatDto;
import com.boatmanagement.entity.Boat;
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
            BoatDto.Response responseDto = new BoatDto.Response(1L, "Sea Explorer", "A sailing boat", Instant.now());

            when(boatRepository.findBySearchTerm(eq(""), any(Pageable.class))).thenReturn(page);
            when(boatMapper.toResponse(boat)).thenReturn(responseDto);

            // Act
            BoatDto.PageResponse result = boatService.findAll("", 0, 10, "createdAt", "desc");

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
            when(boatRepository.findBySearchTerm(any(), any(Pageable.class))).thenReturn(Page.empty());

            // Act
            BoatDto.PageResponse result = boatService.findAll("", 0, 10, "createdAt", "desc");

            // Assert
            assertThat(result.getContent()).isEmpty();
            assertThat(result.getTotalElements()).isEqualTo(0);
        }

        @Test
        @DisplayName("should forward the search term to the repository")
        void should_passSearchTermToRepository_when_searchIsProvided() {
            // Arrange
            when(boatRepository.findBySearchTerm(eq("explorer"), any(Pageable.class))).thenReturn(Page.empty());

            // Act
            boatService.findAll("explorer", 0, 10, "name", "asc");

            // Assert — repository is called with the exact search term
            verify(boatRepository).findBySearchTerm(eq("explorer"), any(Pageable.class));
        }

        @Test
        @DisplayName("should call toResponse mapper for each boat in the page")
        void should_mapEachBoatToResponse_when_pageHasMultipleBoats() {
            // Arrange
            Boat boat1 = Boat.builder().id(1L).name("Boat One").build();
            Boat boat2 = Boat.builder().id(2L).name("Boat Two").build();
            Page<Boat> page = new PageImpl<>(List.of(boat1, boat2));

            when(boatRepository.findBySearchTerm(any(), any())).thenReturn(page);
            when(boatMapper.toResponse(boat1)).thenReturn(new BoatDto.Response(1L, "Boat One", null, null));
            when(boatMapper.toResponse(boat2)).thenReturn(new BoatDto.Response(2L, "Boat Two", null, null));

            // Act
            BoatDto.PageResponse result = boatService.findAll("", 0, 10, "createdAt", "desc");

            // Assert
            assertThat(result.getContent()).hasSize(2);
            verify(boatMapper).toResponse(boat1);
            verify(boatMapper).toResponse(boat2);
        }

        @Test
        @DisplayName("should apply ascending sort when sortDir is 'asc'")
        void should_applyAscendingSort_when_sortDirIsAsc() {
            // Arrange
            when(boatRepository.findBySearchTerm(any(), any(Pageable.class))).thenReturn(Page.empty());

            // Act
            boatService.findAll("", 0, 10, "name", "asc");

            // Assert — pageable must carry ASC direction on the requested field
            verify(boatRepository).findBySearchTerm(eq(""), argThat(pageable ->
                    pageable.getSort().getOrderFor("name") != null &&
                    pageable.getSort().getOrderFor("name").getDirection() == Sort.Direction.ASC
            ));
        }

        @Test
        @DisplayName("should apply descending sort when sortDir is 'desc'")
        void should_applyDescendingSort_when_sortDirIsDesc() {
            // Arrange
            when(boatRepository.findBySearchTerm(any(), any(Pageable.class))).thenReturn(Page.empty());

            // Act
            boatService.findAll("", 0, 10, "name", "desc");

            // Assert
            verify(boatRepository).findBySearchTerm(eq(""), argThat(pageable ->
                    pageable.getSort().getOrderFor("name") != null &&
                    pageable.getSort().getOrderFor("name").getDirection() == Sort.Direction.DESC
            ));
        }

        @Test
        @DisplayName("should default to ascending sort for any sortDir value that is not 'desc'")
        void should_defaultToAscendingSort_when_sortDirIsNotDesc() {
            // Arrange — "ASC", "random", "" all produce ascending order
            when(boatRepository.findBySearchTerm(any(), any(Pageable.class))).thenReturn(Page.empty());

            // Act
            boatService.findAll("", 0, 10, "createdAt", "ASC");

            // Assert
            verify(boatRepository).findBySearchTerm(eq(""), argThat(pageable ->
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
            when(boatRepository.findBySearchTerm(any(), any())).thenReturn(page);

            // Act
            BoatDto.PageResponse result = boatService.findAll("", 0, 10, "createdAt", "desc");

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
            BoatDto.Response expected = new BoatDto.Response(1L, "Sea Explorer", "A sailing boat", Instant.now());

            when(boatRepository.findById(1L)).thenReturn(Optional.of(boat));
            when(boatMapper.toResponse(boat)).thenReturn(expected);

            // Act
            BoatDto.Response result = boatService.findById(1L);

            // Assert
            assertThat(result.getId()).isEqualTo(1L);
            assertThat(result.getName()).isEqualTo("Sea Explorer");
            assertThat(result.getDescription()).isEqualTo("A sailing boat");
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
            BoatDto.Request request = new BoatDto.Request("New Boat", "A new boat");
            Boat unsaved = Boat.builder().name("New Boat").description("A new boat").build();
            Boat saved  = Boat.builder().id(1L).name("New Boat").description("A new boat").build();
            BoatDto.Response expected = new BoatDto.Response(1L, "New Boat", "A new boat", Instant.now());

            when(boatMapper.toEntity(request)).thenReturn(unsaved);
            when(boatRepository.save(unsaved)).thenReturn(saved);
            when(boatMapper.toResponse(saved)).thenReturn(expected);

            // Act
            BoatDto.Response result = boatService.create(request);

            // Assert
            assertThat(result.getId()).isEqualTo(1L);
            assertThat(result.getName()).isEqualTo("New Boat");
            verify(boatMapper).toEntity(request);
            verify(boatRepository).save(unsaved);
            verify(boatMapper).toResponse(saved);
        }

        @Test
        @DisplayName("should persist a boat without a description (optional field)")
        void should_saveBoatWithoutDescription_when_descriptionIsNull() {
            // Arrange — description is optional per the domain model
            BoatDto.Request request = new BoatDto.Request("Boat Without Desc", null);
            Boat unsaved = Boat.builder().name("Boat Without Desc").build();
            Boat saved   = Boat.builder().id(2L).name("Boat Without Desc").build();
            BoatDto.Response expected = new BoatDto.Response(2L, "Boat Without Desc", null, Instant.now());

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
            BoatDto.Request request = new BoatDto.Request("Updated Name", "Updated description");
            Boat existing = Boat.builder().id(1L).name("Old Name").description("Old description").build();
            Boat saved    = Boat.builder().id(1L).name("Updated Name").description("Updated description").build();
            BoatDto.Response expected = new BoatDto.Response(1L, "Updated Name", "Updated description", Instant.now());

            when(boatRepository.findById(1L)).thenReturn(Optional.of(existing));
            when(boatRepository.save(existing)).thenReturn(saved);
            when(boatMapper.toResponse(saved)).thenReturn(expected);

            // Act
            BoatDto.Response result = boatService.update(1L, request);

            // Assert
            assertThat(result.getName()).isEqualTo("Updated Name");
            assertThat(result.getDescription()).isEqualTo("Updated description");

            // Verify that updateEntity was called to mutate the existing entity in-place
            verify(boatMapper).updateEntity(existing, request);
            verify(boatRepository).save(existing);
        }

        @Test
        @DisplayName("should throw BoatNotFoundException without saving when boat does not exist")
        void should_throwBoatNotFoundException_when_boatDoesNotExist() {
            // Arrange
            when(boatRepository.findById(99L)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> boatService.update(99L, new BoatDto.Request("Name", "Desc")))
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

            // Act & Assert
            assertThatThrownBy(() -> boatService.update(1L, new BoatDto.Request("Name", "Desc")))
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
