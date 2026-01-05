package com.geodata.controller;

import com.geodata.model.Review;
import com.geodata.service.ReviewService;
import com.geodata.utils.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@Slf4j
@RestController
@RequestMapping("/api/review")
public class ReviewController {

    private final ReviewService reviewService;

    @Autowired
    public ReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }


    @GetMapping("/personal/number")
    public ResponseEntity<Integer> getTheNumberOfClientReviews() {
        return ResponseEntity.ok(reviewService.getTheNumberOfClientReviews());
    }

    @GetMapping("/personal")
    public ResponseEntity<List<Review>> getClientsReviews() {
        return ResponseEntity.ok(reviewService.getClientReviews());
    }

    /**
     * Retrieves a paginated list of reviews for the authenticated client,
     * filtered by search query, anonymity status, and minimum rating.
     *
     * @param pageable    Pagination and sorting parameters
     * @param query       Search text to filter reviews by comment content (required)
     * @param isAnonymous Optional filter to include only anonymous or non-anonymous reviews
     * @param minRating   Optional filter to include reviews with rating >= minRating
     * @return            ResponseEntity containing paginated reviews matching the criteria
     */
    @GetMapping("/personal/paginated")
    public ResponseEntity<PagedResponse<ReviewDTO>> getClientPersonalReviewPaginated(
            Pageable pageable,
            @RequestParam(name = "searchQuery") String query,
            @RequestParam(required = false) Boolean isAnonymous,
            @RequestParam(required = false) Integer minRating

    ) {
        return ResponseEntity.ok(reviewService.getClientReviewsPaginated(pageable, query, isAnonymous, minRating));
    }

    /**
     * The client should return just the enabled reviews
     * @return all <b>enabled</b> reviews
     */
    @GetMapping("/all")
    public ResponseEntity<List<ReviewDTO>> getAllEnabledReviews() {
        log.info("Returning all the enabled reviews");
        return ResponseEntity.ok(reviewService.getAllEnabledReviews());
    }

    /**
     * The client should return just the enabled reviews
     * @return all <b>enabled</b> reviews
     */
    @GetMapping("/paginated/all")
    public PagedResponse<ReviewDTO> getAllEnabledReviews(
            Pageable pageable,
            @RequestParam(name = "searchQuery") String query,
            @RequestParam(required = false) Boolean isAnonymous,
            @RequestParam(required = false) Integer minRating
            ) {
        return reviewService.getAllEnabledReviewsByQuery(pageable, query, isAnonymous, minRating);
    }

    @GetMapping("/rating/{itemId}")
    public ResponseEntity<Integer> getItemRatingById(@PathVariable("itemId") int itemId) {
        log.info("Returning the rating of the item with the id: {}", itemId);
        return ResponseEntity.ok(reviewService.getItemRatingById(itemId));
    }

    @GetMapping("/reviewable-items/paginated")
    public ResponseEntity<PagedResponse<BorrowActionsDTO>> getReviewableReviewsPaginated(
            Pageable pageable,
            @RequestParam(name = "searchQuery") String query
    ) {
        return ResponseEntity.ok(reviewService.getAllReviewableItemsPaginated(pageable, query));
    }

    @PostMapping("/create")
    public ResponseEntity<String> makeReview(@RequestBody CreateReviewRequest request) {
        return reviewService.makeReview(request);
    }

    @PutMapping("/update")
    public ResponseEntity<String> updateReview(
            @RequestBody UpdateReviewRequest updatedReview
    ) {
        log.info("Trying to update the review with the request {}", updatedReview);
        return reviewService.updateReview(updatedReview);
    }

    @PutMapping("/disable")
    public ResponseEntity<String> disableReview(@RequestParam int reviewId) {
        return reviewService.disableReviewByClient(reviewId);
    }



}
