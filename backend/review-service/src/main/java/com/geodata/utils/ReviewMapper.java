package com.geodata.utils;

import com.geodata.model.Review;

public class ReviewMapper {

    public ReviewMapper() {
        throw new UnsupportedOperationException("Utility class");
    }

    public static ReviewDTO toDto(Review review, LibraryItemDTO itemDTO, String clientName) {
        return ReviewDTO.builder()
                .reviewId(review.getReviewId())
                .comment(review.getComment())
                .rating(review.getRating())
                .item(itemDTO)
                .clientName(clientName)
                .isAnonymous(review.isAnonymous())
                .isEnabled(review.isEnabled())
                .updatedAt(review.getUpdatedAt())
                .createdAt(review.getCreatedAt())
                .build();
    }


}
