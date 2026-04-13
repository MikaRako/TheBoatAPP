package com.boatmanagement.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Entity
@Table(name = "boats")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Boat {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "boat_seq")
    @SequenceGenerator(name = "boat_seq", sequenceName = "boat_sequence", allocationSize = 1)
    private Long id;

    @NotBlank(message = "Boat name is required")
    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BoatStatus status = BoatStatus.IN_PORT;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BoatType type = BoatType.YACHT;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
