package com.geodata.service;

import java.util.*;

import com.geodata.utils.ReviewDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class ReviewManagementService {

    private final ReviewService reviewService;

    @Autowired
    public ReviewManagementService(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    public ResponseEntity<String> disableReviewByAdmin(int reviewId) {
        log.info("Disabling review #{} by admin", reviewId);
        try {
            reviewService.disableReview(reviewId);
            return ResponseEntity.ok("Review disabled successfully");
        } catch (Exception e) {
            log.error("Failed to disable review #{}", reviewId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to disable review: " + e.getMessage());
        }
    }

    public ResponseEntity<List<ReviewDTO>> getAllReviews() {
        return ResponseEntity.ok(reviewService.getAllReviews());
    }
}
