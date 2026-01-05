package com.geodata.utils;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class UpdateReviewRequest {
    private int reviewId;
    private String comment;
    private int rating;
    private Boolean isAnonymous;
}
