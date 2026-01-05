package com.geodata.controller;

import com.geodata.service.ReviewManagementService;
import com.geodata.utils.ReviewDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RequestMapping("/api/review/librarian")
@RestController
public class ReviewManagementController {

    private final ReviewManagementService reviewManagementService;

    @Autowired
    public ReviewManagementController(ReviewManagementService reviewManagementService) {
        this.reviewManagementService = reviewManagementService;
    }

    @GetMapping("/all")
    public ResponseEntity<List<ReviewDTO>> getAllReviews() {
        return (reviewManagementService.getAllReviews());
    }

    @PutMapping("/disable")
    public ResponseEntity<String> disableReview(@RequestParam(name = "reviewId") int reviewId) {
        return reviewManagementService.disableReviewByAdmin(reviewId);
    }

}
