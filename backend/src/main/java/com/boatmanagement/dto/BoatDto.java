package com.boatmanagement.dto;

import com.boatmanagement.entity.BoatStatus;
import com.boatmanagement.entity.BoatType;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.Instant;

public class BoatDto {

    @Schema(description = "Boat response object")
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Response {
        @Schema(description = "Unique identifier", example = "1")
        private Long id;

        @Schema(description = "Boat name", example = "Sea Explorer")
        private String name;

        @Schema(description = "Boat description", example = "A luxury sailing boat")
        private String description;

        @Schema(description = "Boat status", example = "IN_PORT")
        private BoatStatus status;

        @Schema(description = "Boat type", example = "YACHT")
        private BoatType type;

        @Schema(description = "Creation timestamp")
        @JsonFormat(shape = JsonFormat.Shape.STRING)
        private Instant createdAt;
    }

    @Schema(description = "Boat create/update request")
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Request {
        @Schema(description = "Boat name", example = "Sea Explorer", required = true)
        @NotBlank(message = "Name is required")
        @Size(min = 1, max = 255, message = "Name must be between 1 and 255 characters")
        private String name;

        @Schema(description = "Boat description", example = "A luxury sailing boat")
        @Size(max = 2000, message = "Description must not exceed 2000 characters")
        private String description;

        @Schema(description = "Boat status", example = "IN_PORT")
        @NotNull(message = "Status is required")
        private BoatStatus status;

        @Schema(description = "Boat type", example = "YACHT")
        @NotNull(message = "Type is required")
        private BoatType type;
    }

    @Schema(description = "Paginated boats response")
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class PageResponse {
        private java.util.List<Response> content;
        private int page;
        private int size;
        private long totalElements;
        private int totalPages;
        private boolean last;
    }
}
