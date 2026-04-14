package com.boatmanagement.controller;

import com.boatmanagement.dto.BoatDto;
import com.boatmanagement.entity.BoatStatus;
import com.boatmanagement.entity.BoatType;
import com.boatmanagement.service.BoatService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/boats")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Boats", description = "Boat management API")
@SecurityRequirement(name = "bearerAuth")
public class BoatController {

    private final BoatService boatService;

    @GetMapping
    @Operation(summary = "Get all boats", description = "Returns a paginated list of boats with optional search")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Successful retrieval"),
        @ApiResponse(responseCode = "400", description = "Invalid query parameter"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<BoatDto.PageResponse> getAllBoats(
            @Parameter(description = "Search term for name or description")
            @RequestParam(required = false, defaultValue = "") String search,
            @Parameter(description = "Filter by status (UNDERWAY, IN_PORT, MAINTENANCE)")
            @RequestParam(required = false) @Nullable BoatStatus status,
            @Parameter(description = "Filter by type (SAILBOAT, TRAWLER, CARGO_SHIP, YACHT, FERRY)")
            @RequestParam(required = false) @Nullable BoatType type,
            @Parameter(description = "Page number (0-indexed)")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size")
            @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "Sort field")
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @Parameter(description = "Sort direction (asc/desc)")
            @RequestParam(defaultValue = "desc") String sortDir) {

        return ResponseEntity.ok(boatService.findAll(search, status, type, page, size, sortBy, sortDir));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get boat by ID")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Boat found"),
        @ApiResponse(responseCode = "404", description = "Boat not found"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<BoatDto.Response> getBoatById(@PathVariable @NonNull Long id) {
        return ResponseEntity.ok(boatService.findById(id));
    }

    @PostMapping
    @Operation(summary = "Create a new boat")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Boat created"),
        @ApiResponse(responseCode = "400", description = "Validation error"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<BoatDto.Response> createBoat(@Valid @RequestBody BoatDto.Request request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(boatService.create(request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update an existing boat")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Boat updated"),
        @ApiResponse(responseCode = "400", description = "Validation error"),
        @ApiResponse(responseCode = "404", description = "Boat not found"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<BoatDto.Response> updateBoat(
            @PathVariable @NonNull Long id,
            @Valid @RequestBody BoatDto.Request request) {
        return ResponseEntity.ok(boatService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a boat")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Boat deleted"),
        @ApiResponse(responseCode = "404", description = "Boat not found"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<Void> deleteBoat(@PathVariable @NonNull Long id) {
        boatService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
