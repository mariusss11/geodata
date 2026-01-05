package com.geodata.service;

import com.geodata.exceptions.*;
import com.geodata.model.Review;
import com.geodata.repository.ReviewRepository;
import com.geodata.security.jwt.JwtUtils;
import com.geodata.utils.*;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Slf4j
@Service
public class ReviewService {

    @Value("${services.borrowService}")
    private String borrowService;

    @Value("${services.authService}")
    private String authService;

    @Value("${services.itemService}")
    private String itemService;

    @Value("${services.clientService}")
    private String clientService;

    private final ReviewRepository reviewRepository;
    private final RestTemplate restTemplate;
    private final JwtUtils jwtUtils;
    private final HttpServletRequest httpRequest;

    private static final String HTTP = "http://";
    private static final String ITEM_BY_ID_ENDPOINT = "/api/items/{itemId}";

    @Autowired
    public ReviewService(ReviewRepository reviewRepository, RestTemplate restTemplate, JwtUtils jwtUtils, HttpServletRequest httpRequest) {
        this.reviewRepository = reviewRepository;
        this.restTemplate = restTemplate;
        this.jwtUtils = jwtUtils;
        this.httpRequest = httpRequest;
    }

    Review getReviewById(int reviewId) {
        return reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ReviewNotFoundException("Review not found with the Id: " + reviewId));
    }

    public List<Review> getClientReviews() {
        Client client = getClient();
        log.info("Returning client reviews: {}", client);
        return reviewRepository.findAllByClientId(client.getClientId());
    }

    /**
     * Retrieves paginated reviews for the currently authenticated client, applying optional filters.
     * Validates client status and formats query for SQL LIKE matching.
     *
     * @param pageable   Pagination and sorting configuration
     * @param query      Search keyword(s) for filtering review comments
     * @param anonymity  Filter reviews by anonymity status (true, false, or null for no filter)
     * @param minRating  Minimum rating filter (inclusive), nullable
     * @return           PaginatedResponse wrapper with filtered reviews
     * @throws AccessDeniedException if client's account is disabled
     */

    public PagedResponse<ReviewDTO> getClientReviewsPaginated(Pageable pageable, String query, Boolean anonymity, Integer minRating) {
        Client client = getClient();
        if (!client.isEnabled())
            throw new AccessDeniedException("User is enabled and should not access thi endpoint");
        log.info("Returning client paginated reviews");

        String formattedQuery = "%" + query + "%";
        List<Review> allClientReviews = reviewRepository.findClientReviewsByFilters(client.getClientId(), formattedQuery, anonymity, minRating);

        List<ReviewDTO> allClientReviewsDto = new ArrayList<>();
        for (Review review : allClientReviews) {

            ResponseEntity<LibraryItemDTO> itemServiceResponse = restTemplate.exchange(
                    HTTP + itemService + ITEM_BY_ID_ENDPOINT,
                    HttpMethod.GET,
                    getEntity(),
                    LibraryItemDTO.class,
                    review.getItemId()
            );

            // check if the response is valid
            if (itemServiceResponse.getBody() == null)
                throw new ItemServiceException("Item service response is empty");

            allClientReviewsDto.add(
                    ReviewMapper.toDto(review, itemServiceResponse.getBody(), client.getName())
            );
        }

        int pageNumber = pageable.getPageNumber();
        int pageSize = pageable.getPageSize();

        int start = Math.min(pageNumber * pageSize, allClientReviewsDto.size());
        int end = Math.min(start + pageSize, allClientReviewsDto.size());

        List<ReviewDTO> pageList = allClientReviewsDto.subList(start, end);
        Page<ReviewDTO> mappedReviewPage = new PageImpl<>(pageList, pageable, allClientReviewsDto.size());
        return new PagedResponse<>(mappedReviewPage);
    }

    public List<ReviewDTO> getAllEnabledReviews() {
        List<ReviewDTO> allReviews = getAllReviews();
        return allReviews.stream().filter(ReviewDTO::isEnabled).toList();
    }

    public ResponseEntity<String> updateReview(UpdateReviewRequest updatedReview) {
        try {
            Review newReview = getReviewById(updatedReview.getReviewId());

            newReview.setRating(updatedReview.getRating());
            newReview.setComment(updatedReview.getComment());
            newReview.setAnonymous(updatedReview.getIsAnonymous());
            newReview.setUpdatedAt(LocalDateTime.now());

            reviewRepository.save(newReview);
            log.info("The new saved review: {}", newReview);
            return ResponseEntity.ok("Review updated successfully");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Failed to updated review");
        }
    }

    public List<ReviewDTO> getAllReviews() {
        List<Review> reviews = reviewRepository.findAll();
        List<ReviewDTO> reviewsDTO = new ArrayList<>();

        HttpHeaders getHeaders = new HttpHeaders();
        getHeaders.setContentType(MediaType.APPLICATION_JSON);
        getHeaders.setBearerAuth(getAuthToken());

        HttpEntity<Void> entity = new HttpEntity<>(getHeaders);

        for (Review review : reviews) {
            LibraryItemDTO item = getItem(review.getItemId());

            ResponseEntity<String> clientServiceResponse = restTemplate.exchange(
                    HTTP + clientService + "/api/client/name/{clientId}",
                    HttpMethod.GET,
                    entity,
                    String.class,
                    review.getClientId()
            );

            if (clientServiceResponse.getBody() == null)
                throw new ClientServiceException("Client service response is empty");

            // map to the DTO with the item
            reviewsDTO.add(ReviewMapper.
                    toDto(review, item, clientServiceResponse.getBody()));
        }
        return  reviewsDTO;
    }

    public ResponseEntity<String> makeReview(CreateReviewRequest request) {
        log.info("Trying to make a review with the request: {}", request);
        // fetch the client
        Client client;
        try {
            client = getClient();
        } catch (ClientNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (ClientServiceException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Failed to fetch the client");
        }


        // fetch item
        LibraryItemDTO item;
        try {
            item = getItem(request.getItemId());
        } catch (ItemNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Failed to fetch the item");
        }

        if (!item.isEnabled())
            throw new InvalidItemException("Cannot make review on this item");

        log.info("The items is: {}", item);

        // check if the client borrowed the item
        try {
            boolean hasBorrowed = hasClientBorrowedTheItem(client.getClientId(), request.getItemId());
            if (!hasBorrowed)
                return ResponseEntity
                        .status(HttpStatus.BAD_REQUEST)
                        .body("You can only review items you have borrowed.");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }

        log.info("Saving the review");
        reviewRepository.save(
                Review.builder()
                        .clientId(client.getClientId())
                        .itemId(request.getItemId())
                        .comment(request.getComment())
                        .isAnonymous(request.isAnonymous())
                        .isEnabled(true)
                        .rating(request.getRating())
                        .createdAt(LocalDateTime.now())
                        .updatedAt(LocalDateTime.now())
                        .build()
        );
        return ResponseEntity.ok("Review saved successfully");
    }

    Client getClient() {
        HttpEntity<Void> entity = getEntity();
        try {
            // make a request to the client service to get the client
            ResponseEntity<Client> clientServiceResponse = restTemplate.exchange(
                    HTTP + clientService + "/api/client",
                    HttpMethod.GET,
                    entity,
                    Client.class
            );

            if (clientServiceResponse.getBody() == null)
                throw new ClientServiceException("Client service returned empty body");

            return clientServiceResponse.getBody();
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode().isSameCodeAs(HttpStatus.NOT_FOUND)) {
                throw new ClientNotFoundException("Client not found to review the item");
            }
            throw new ClientServiceException("Client service error: " + e.getMessage());
        }
    }

    LibraryItemDTO getItem(int itemId) {
        HttpEntity<?> entity = getEntity();
        try {
            ResponseEntity<LibraryItemDTO> itemServiceResponse = restTemplate.exchange(
                    HTTP + itemService + ITEM_BY_ID_ENDPOINT,
                    HttpMethod.GET,
                    entity,
                    LibraryItemDTO.class,
                    itemId
            );

            if (itemServiceResponse.getBody() == null) {
                throw new ItemServiceException("Response body is empty");
            }

            log.info("Item successfully fetched from the item-service: {}", itemServiceResponse.getBody());
            return itemServiceResponse.getBody();
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode().isSameCodeAs(HttpStatus.NOT_FOUND)) {
                throw new ItemNotFoundException("Item not found to be reviewed");
            }
            throw new ItemServiceException("Item service error: " + e.getMessage());
        }
    }

    boolean hasClientBorrowedTheItem(int clientId, int itemId) {
        HttpEntity<?> entity = getEntity();
        ResponseEntity<Boolean> borrowServiceResponse = restTemplate.exchange(
                HTTP + borrowService +
                        "/api/borrows/hasBorrowed/{clientId}/{itemId}",
                HttpMethod.GET,
                entity,
                Boolean.class,
                clientId,
                itemId
        );

        Boolean body = borrowServiceResponse.getBody();
        if (body == null) {
            throw new BorrowServiceException("Borrow response body is empty");
        }
        return body;
    }

    String getAuthToken() {
        String token = jwtUtils.getToken(httpRequest.getHeader("Authorization"));
        if (token == null)
            throw new IllegalStateException("Authorization token is missing or invalid");
        return token;
    }

    public ResponseEntity<String> disableReviewByClient(int reviewId) {
        log.info("Trying to disable review #{} by client", reviewId);

        // fetch the client
        Client client;
        try {
            client = getClient();
        } catch (ClientNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Failed to fetch the client");
        }

        // get the review
        Review review = getReviewById(reviewId);

        // check if the client made the review
        if (review.getClientId() != client.getClientId())
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body("Cannot delete a review written by other client");

        disableReview(reviewId);
        return ResponseEntity.ok("Review disabled successfully");
    }

    void disableReview(int reviewId) {
        Review reviewToDisable = getReviewById(reviewId);
        reviewToDisable.setEnabled(false);
        reviewRepository.save(reviewToDisable);
    }

    private HttpEntity<Void> getEntity() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(getAuthToken());
        return new HttpEntity<>(headers);
    }

    public Integer getItemRatingById(int itemId) {
        List<Review> itemReviews = reviewRepository.findAllByItemId(itemId);
        if (itemReviews.isEmpty())
            return 0;

        int sum = 0;
        for (Review review : itemReviews) {
            sum += review.getRating();
        }

        return sum / itemReviews.size();
    }

    public Integer getTheNumberOfClientReviews() {
        return getClientReviews().size();
    }

    public PagedResponse<ReviewDTO> getAllEnabledReviewsByQuery(Pageable pageable, String query, Boolean anonymity, Integer minRating) {
        log.info("Returning all enabled reviews by: \nQuery: {} \n anonymity: {} \n minRating: {}", query, anonymity, minRating);

        // Get the ids from teh itemService
        ResponseEntity<Set<Integer>> itemServiceIdListResponse = restTemplate.exchange(
                HTTP + itemService + "/api/items/list/id?query={query}",
                HttpMethod.GET,
                getEntity(),
                new ParameterizedTypeReference<>() {
                },
                query
        );

        if (itemServiceIdListResponse.getBody() == null)
            throw new ItemServiceException("Item service response is empty");

        Set<Integer> idsList = itemServiceIdListResponse.getBody();

        List<Review> simpleReviews =
                reviewRepository.findAllByItemIdInAndEnabledTrue(idsList);

        List<ReviewDTO> mappedReviews = new ArrayList<>();
        for (Review review : simpleReviews) {
            // Get the client name
            ResponseEntity<String> clientServiceResponse = restTemplate.exchange(HTTP + clientService + "/api/client/name/{clientId}",
                    HttpMethod.GET, getEntity(), String.class, review.getClientId());

            if (clientServiceResponse.getBody() == null)
                throw new ClientServiceException("Client Service response is empty");

            ResponseEntity<LibraryItemDTO> itemServiceResponse = restTemplate.exchange(
                    HTTP + itemService + ITEM_BY_ID_ENDPOINT,
                    HttpMethod.GET,
                    getEntity(),
                    LibraryItemDTO.class,
                    review.getItemId()
            );

            // check if the response is valid
            if (itemServiceResponse.getBody() == null)
                throw new ItemServiceException("Item service response is empty");

            // Map the client name to the review dto object
            ReviewDTO dto = ReviewMapper.toDto(review, itemServiceResponse.getBody(), clientServiceResponse.getBody());

            boolean matchesAnonymity = anonymity == null || review.isAnonymous() == anonymity;
            boolean matchesRating = minRating == null || review.getRating() >= minRating;

            if (matchesAnonymity && matchesRating)
                mappedReviews.add(dto);

        }
        int pageNumber = pageable.getPageNumber();
        int pageSize = pageable.getPageSize();

        int start = Math.min(pageNumber * pageSize, mappedReviews.size());
        int end = Math.min(start + pageSize, mappedReviews.size());

        List<ReviewDTO> pageList = mappedReviews.subList(start, end);
        Page<ReviewDTO> mappedReviewPage = new PageImpl<>(pageList, pageable, mappedReviews.size());

        // Return the review dto list
        return new PagedResponse<>(mappedReviewPage);
    }


    public PagedResponse<BorrowActionsDTO> getAllReviewableItemsPaginated(Pageable pageable, String query) {
        log.info("Getting all reviewable items paginated");
        Client client = getClient();
        if (!client.isEnabled())
            throw new AccessDeniedException("User is enabled and should not access thi endpoint");

        List<BorrowActionsDTO> clientBorrowActionsFiltered = findClientReviewableItemsByQuery(query, client.getClientId());

        log.info("Starting the pagination of the borrowActions: {}", clientBorrowActionsFiltered);
        int pageNumber = pageable.getPageNumber();
        int pageSize = pageable.getPageSize();

        int start = Math.min(pageNumber * pageSize, clientBorrowActionsFiltered.size());
        int end = Math.min(start + pageSize, clientBorrowActionsFiltered.size());

        List<BorrowActionsDTO> pageList = clientBorrowActionsFiltered.subList(start, end);
        Page<BorrowActionsDTO> mappedBorrowActionsPage = new PageImpl<>(pageList, pageable, clientBorrowActionsFiltered.size());
        return new PagedResponse<>(mappedBorrowActionsPage);
    }

    private List<BorrowActionsDTO> findClientReviewableItemsByQuery(String query, int clientId) {
        log.info("Getting client reviewable items by query");
        String url = HTTP + borrowService + "/api/borrows/returned-items/filtered?clientId={clientId}&query={query}";
//        log.info("Final URL: {}", url);
        ResponseEntity<List<BorrowActionsDTO>> borrowServiceResponse = restTemplate.exchange(
                url,
                HttpMethod.GET,
                getEntity(),
                new ParameterizedTypeReference<>() {},
                Map.of(
                        "clientId", clientId,
                        "query", query
                )
        );

        if (borrowServiceResponse.getBody() == null)
            throw new BorrowServiceException("Borrow service response is empty");

        return borrowServiceResponse.getBody();
    }
}
