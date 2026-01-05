package com.geodata.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Builder
@Entity
@AllArgsConstructor
@NoArgsConstructor
@Data
@Table(
        name = "reviews",
        schema = "review"
)
public class Review {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int reviewId;

    @Column(name = "client_id")
    private int clientId;

    @Column(name = "item_id")
    private int itemId;

    private String comment;
    private int rating;

    @Column(name = "is_anonymous")
    private boolean isAnonymous;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "is_enabled", columnDefinition = "BOOLEAN DEFAULT TRUE")
    private boolean isEnabled;

}
