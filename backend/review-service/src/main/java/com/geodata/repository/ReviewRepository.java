package com.geodata.repository;

import com.geodata.model.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.*;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Integer> {

    List<Review> findAllByClientId(int clientId);

    List<Review> findAllByItemId(int itemId);

    @Query(value = "SELECT * FROM reviews r WHERE r.item_id IN :ids AND r.is_enabled = true", nativeQuery = true)
    List<Review> findAllByItemIdInAndEnabledTrue(@Param("ids") Set<Integer> ids);

    /**
     * Fetches paginated reviews for a specific client, filtered by comment content, anonymity, and rating.
     * Supports optional filters by allowing null parameters to bypass criteria.
     * Use native SQL with ILIKE for case-insensitive pattern matching.
     *
     * @param clientId   Client's unique identifier
     * @param query      Search pattern for review comments (e.g., '%term%'), nullable to disable filter
     * @param anonymity  Filter for anonymous status: true (only anonymous), false (only non-anonymous), null (all)
     * @param minRating  Minimum rating threshold, nullable to disable filter
     * @return           Page of reviews meeting the filter criteria
     */
    @Query(value = """
  SELECT * FROM reviews r
  WHERE r.client_id = :clientId
    AND r.is_enabled = true
    AND (:query IS NULL OR r.comment ILIKE :query)
    AND (:anonymity IS NULL OR r.is_anonymous = :anonymity)
    AND (:minRating IS NULL OR r.rating >= :minRating)
""", nativeQuery = true)
    List<Review> findClientReviewsByFilters(
            @Param("clientId") int clientId,
            @Param("query") String query,
            @Param("anonymity") Boolean anonymity,
            @Param("minRating") Integer minRating
    );

}
