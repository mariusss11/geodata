package com.geodata.model;

import jakarta.persistence.*;
import lombok.*;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Data
@Slf4j
@Entity
@Getter
@Setter
@Table(name = "maps")
public class Map {
    @Setter
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "map_id")
    private int id;

    private String name;

    private int year;

    @Column(name = "is_enabled", columnDefinition = "BOOLEAN DEFAULT TRUE")
    private boolean isEnabled = true;

    @Column(name = "availability_status")
    private String availabilityStatus;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

}
