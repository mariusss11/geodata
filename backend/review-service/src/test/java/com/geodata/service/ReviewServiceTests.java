package com.geodata.service;

import com.geodata.exceptions.*;
import com.geodata.model.Review;
import com.geodata.repository.ReviewRepository;
import com.geodata.security.jwt.JwtUtils;
import com.geodata.utils.Client;
import com.geodata.utils.CreateReviewRequest;
import com.geodata.utils.LibraryItemDTO;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

//  [Find about LENIENT](https://nicolas.riousset.com/category/software-methodologies/fixing-mockito-unnecessarystubbingexception-with-junit5/)
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ReviewServiceTests {

    @InjectMocks
    private ReviewService reviewService;

    @Mock
    private ReviewRepository reviewRepository;

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private JwtUtils jwtUtils;

    @Mock
    private HttpServletRequest httpServletRequest;

    private static final String AUTH_TOKEN = "valid.token";

    @BeforeEach
    void setUp() {
        // Mock the Authorization header
        when(httpServletRequest.getHeader("Authorization"))
                .thenReturn("Bearer " + AUTH_TOKEN);

        // Mock JWT utils behavior
        when(jwtUtils.getToken(anyString()))
                .thenReturn(AUTH_TOKEN);
    }

    @Test
    void getAuthToken_WithValidHeader_ReturnsToken() {
        // Arrange
        when(httpServletRequest.getHeader("Authorization"))
                .thenReturn("Bearer " + AUTH_TOKEN);
        when(jwtUtils.getToken("Bearer " + AUTH_TOKEN))
                .thenReturn(AUTH_TOKEN);

        // Act
        String token = reviewService.getAuthToken();

        // Assert
        assertEquals(AUTH_TOKEN, token);
    }

    @Test
    void getAuthToken_whenAuthTokenIsNull_returnsException() {
        // Arrange
        when(httpServletRequest.getHeader("Authorization"))
                .thenReturn("Invalid token");
        when(jwtUtils.getToken("Invalid token"))
                .thenReturn(null);

        // Act & Assert
        assertThrows(IllegalStateException.class, () ->
                reviewService.getAuthToken());
    }

    @Test
    void getReviewById_WhenValidRequest_ShouldReturnReview() {
        // Arrange
        Review mockReview = new Review(1, 1, 1,
                "Great book!", 10, false,
                LocalDateTime.now(), LocalDateTime.now(), true);

        when(reviewRepository.findById(1))
                .thenReturn(Optional.of(mockReview));

        // Act
        Review review = reviewService.getReviewById(1);

        // Assert
        assertNotNull(review);
        assertEquals(10, review.getRating());
        verify(reviewRepository).findById(anyInt());
    }

    @Test
    void getClientReviews_WhenValidRequest_ShouldReturnAllClientReviews() {
        // Arrange
        Client mockClient = new Client("John Doe", "johndoe@gmail.com");
        List<Review> mockReviews = Arrays.asList(
                new Review(1, 1, 1, "Great book!", 10, false, LocalDateTime.now(), LocalDateTime.now(), true),
                new Review(2, 1, 2, "Average read", 5, true, LocalDateTime.now(), LocalDateTime.now(), true)
        );

        when(restTemplate.exchange(anyString(), any(), any(), eq(Client.class)))
                .thenReturn(new ResponseEntity<>(mockClient, HttpStatus.OK));

        when(reviewRepository.findAllByClientId(anyInt())).thenReturn(mockReviews);

        // Act
        List<Review> result = reviewService.getClientReviews();

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("Great book!", result.get(0).getComment());
        assertEquals(5, result.get(1).getRating());
        verify(reviewRepository).findAllByClientId(anyInt());
    }

    @Test
    void getClientReviews_WhenValidRequest_ShouldReturnClientNotFoundException() {
            when(restTemplate.exchange(anyString(), any(), any(), eq(Client.class)))
                .thenThrow(new ClientNotFoundException("Client not found"));

        // Act
        assertThrows(ClientNotFoundException.class, () ->
                reviewService.getClientReviews());

        verify(reviewRepository, never()).findAllByClientId(anyInt());
    }

//    @Test
//    void getAllReviews_WhenValidRequest_ShouldReturnAllReviews() {
//        // Arrange
//        List<Review> mockReviews = Arrays.asList(
//                new Review(1, 1, 1, "Great book!", 10, false, LocalDateTime.now(), LocalDateTime.now(), true),
//                new Review(2, 5, 2, "Average read", 5, true, LocalDateTime.now(), LocalDateTime.now(), true)
//        );
//        LibraryItemDTO mockItem = new LibraryItemDTO();
//        Client mockClient = new Client();
//
//        when(reviewRepository.findAll()).thenReturn(mockReviews);
//
//        when(restTemplate.exchange(anyString(), any(), any(), eq(LibraryItemDTO.class), anyInt()))
//                .thenReturn(ResponseEntity.ok(mockItem));
//
//        when(restTemplate.exchange(anyString(), any(), any(), eq(Client.class), anyInt()))
//                .thenReturn(ResponseEntity.ok(mockClient));
//
//        // Act
//        List<ReviewDTO> allReviews = reviewService.getAllReviews();
//
//        // Assert
//        assertNotNull(allReviews);
//        assertEquals(2, allReviews.size());
//        assertEquals("Great book!", allReviews.get(0).getComment());
//        assertEquals(5, allReviews.get(1).getRating());
//        verify(reviewRepository).findAll();
//    }


    // MAKE REVIEW

    @Test
    void makeReview_WhenValidRequest_ShouldReturnSuccessResponse() {
        // Arrange
        CreateReviewRequest request = new CreateReviewRequest(1, "Great book!", 5, false);
        Client mockClient = new Client("John Doe", "johndoe@gmail.com");
        LibraryItemDTO mockItem = new LibraryItemDTO("Sample Book", "Author", "Book");

        when(restTemplate.exchange(anyString(), any(), any(), eq(Client.class)))
                .thenReturn(new ResponseEntity<>(mockClient, HttpStatus.OK));

        when(restTemplate.exchange(anyString(), any(), any(), eq(LibraryItemDTO.class), anyInt()))
                .thenReturn(new ResponseEntity<>(mockItem, HttpStatus.OK));

        when(restTemplate.exchange(anyString(), any(), any(), eq(Boolean.class), anyInt(), anyInt()))
                .thenReturn(new ResponseEntity<>(true, HttpStatus.OK));

        when(reviewRepository.save(any(Review.class))).thenReturn(new Review());

        // Act
        ResponseEntity<?> response = reviewService.makeReview(request);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Review saved successfully", response.getBody());
        verify(reviewRepository).save(any(Review.class));
    }

    @Test
    void makeReview_WhenValidRequest_ShouldReturnClientNotFoundException() {
        // Arrange
        CreateReviewRequest request = new CreateReviewRequest(1, "Great book!", 5, false);

        when(restTemplate.exchange(anyString(), any(), any(), eq(Client.class)))
                .thenThrow(new ClientNotFoundException("Client not found"));

        // Act
        ResponseEntity<?> response = reviewService.makeReview(request);

        // Assert
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertEquals("Client not found", response.getBody());
        verify(reviewRepository, never()).save(any(Review.class));
    }

    @Test
    void makeReview_WhenValidRequest_ShouldReturnItemNotFoundException() {
        // Arrange
        CreateReviewRequest request = new CreateReviewRequest(1, "Great book!", 5, false);
        Client mockClient = new Client("John Doe", "johndoe@gmail.com");

        when(restTemplate.exchange(anyString(), any(), any(), eq(Client.class)))
                .thenReturn(new ResponseEntity<>(mockClient, HttpStatus.OK));


        when(restTemplate.exchange(anyString(), any(), any(), eq(LibraryItemDTO.class), anyInt()))
                .thenThrow(new ItemNotFoundException("Item not found"));

        // Act
        ResponseEntity<?> response = reviewService.makeReview(request);

        // Assert
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertEquals("Item not found", response.getBody());
        verify(reviewRepository, never()).save(any(Review.class));
    }

    @Test
    void makeReview_WhenValidRequest_ShouldReturnCannotReviewItemThatWasNotBorrowed() {
        // Arrange
        CreateReviewRequest request = new CreateReviewRequest(1, "Great book!", 5, false);
        Client mockClient = new Client("John Doe", "johndoe@gmail.com");
        LibraryItemDTO mockItem = new LibraryItemDTO("Sample Book", "Author", "Book");

        when(restTemplate.exchange(anyString(), any(), any(), eq(Client.class)))
                .thenReturn(new ResponseEntity<>(mockClient, HttpStatus.OK));

        when(restTemplate.exchange(anyString(), any(), any(), eq(LibraryItemDTO.class), anyInt()))
                .thenReturn(new ResponseEntity<>(mockItem, HttpStatus.OK));

        when(restTemplate.exchange(anyString(), any(), any(), eq(Boolean.class), anyInt(), anyInt()))
                .thenReturn(new ResponseEntity<>(false, HttpStatus.OK));

        // Act
        ResponseEntity<?> response = reviewService.makeReview(request);

        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("You can only review items you have borrowed.", response.getBody());
        verify(reviewRepository, never()).save(any(Review.class));
    }

    @Test
    void makeReview_whenFailedToFetchClient_ShouldReturnBadRequest() {
        // Arrange
        CreateReviewRequest request = new CreateReviewRequest(1, "Great book!", 5, false);

        when(restTemplate.exchange(anyString(), any(), any(), eq(Client.class)))
                .thenThrow(new HttpClientErrorException(HttpStatus.BAD_REQUEST));

        // Act
        ResponseEntity<?> response = reviewService.makeReview(request);

        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Failed to fetch the client", response.getBody());
        verify(reviewRepository, never()).save(any(Review.class));
    }

    @Test
    void makeReview_whenFailedToFetchItem_ShouldReturnBadRequest() {
        // Arrange
        CreateReviewRequest request = new CreateReviewRequest(1, "Great book!", 5, false);
        Client mockClient = new Client("John Doe", "johndoe@gmail.com");

        when(restTemplate.exchange(anyString(), any(), any(), eq(Client.class)))
                .thenReturn(new ResponseEntity<>(mockClient, HttpStatus.OK));

        when(restTemplate.exchange(anyString(), any(), any(), eq(LibraryItemDTO.class)))
                .thenThrow(new HttpClientErrorException(HttpStatus.BAD_REQUEST));

        // Act
        ResponseEntity<?> response = reviewService.makeReview(request);

        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Failed to fetch the item", response.getBody());
        verify(reviewRepository, never()).save(any(Review.class));
    }


    @Test
    void makeReview_whenResponseBodyIsEmpty_shouldThrowRuntimeException() {
        // Arrange
        CreateReviewRequest request = new CreateReviewRequest(1, "Great book!", 5, false);
        Client mockClient = new Client("John Doe", "johndoe@gmail.com");
        LibraryItemDTO mockItem = new LibraryItemDTO("Sample Book", "Author", "Book");
        ResponseEntity<Boolean> emptyResponse = ResponseEntity.ok().build();

        when(restTemplate.exchange(anyString(), any(), any(), eq(Client.class)))
                .thenReturn(new ResponseEntity<>(mockClient, HttpStatus.OK));

        when(restTemplate.exchange(anyString(), any(), any(), eq(LibraryItemDTO.class), anyInt()))
                .thenReturn(new ResponseEntity<>(mockItem, HttpStatus.OK));

        when(restTemplate.exchange(anyString(), any(), any(), eq(Boolean.class), anyInt(), anyInt()))
                .thenReturn(emptyResponse);

        // Act
        ResponseEntity<?> response = reviewService.makeReview(request);

        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Borrow response body is empty", response.getBody());
        verify(reviewRepository, never()).save(any(Review.class));
    }


    @Test
    void makeReview_WhenItemIsDisabled_ShouldReturnException() {
        // Arrange
        CreateReviewRequest request = new CreateReviewRequest(1, "Great book!", 5, false);
        Client mockClient = new Client("John Doe", "johndoe@gmail.com");
        LibraryItemDTO mockItem = new LibraryItemDTO("Sample Book", "Author", "Book");
        mockItem.setEnabled(false);

        when(restTemplate.exchange(anyString(), any(), any(), eq(Client.class)))
                .thenReturn(new ResponseEntity<>(mockClient, HttpStatus.OK));

        when(restTemplate.exchange(anyString(), any(), any(), eq(LibraryItemDTO.class), anyInt()))
                .thenReturn(new ResponseEntity<>(mockItem, HttpStatus.OK));

        // Act
        InvalidItemException invalidItemException = assertThrows(InvalidItemException.class, () -> reviewService.makeReview(request));

        // Assert
        assertEquals("Cannot make review on this item", invalidItemException.getMessage());
        verify(reviewRepository, never()).save(any(Review.class));
    }

    // HAD CLIENT BORROWED THE ITEM

    @Test
    void hasClientBorrowedTheItem_whenResponseIsEmpty_shouldReturnRuntimeException() {
        int clientId = 1;
        int itemId = 1;

        ResponseEntity<Boolean> emptyResponse = ResponseEntity.ok().build();

        when(restTemplate.exchange(anyString(), any(), any(), eq(Boolean.class), eq(clientId), eq(itemId)))
                .thenReturn(emptyResponse);

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> reviewService.hasClientBorrowedTheItem(clientId, itemId)
        );

        assertEquals("Borrow response body is empty", exception.getMessage());
    }

    @Test
    void hasClientBorrowedTheItem_whenClientDidNotBorrowItem() {
        int clientId = 1;
        int itemId = 1;
        when(restTemplate.exchange(anyString(), any(), any(), eq(Boolean.class), eq(clientId), eq(itemId)))
                .thenReturn(ResponseEntity.ok(anyBoolean()));

        assertFalse(reviewService.hasClientBorrowedTheItem(clientId, itemId));
    }


    //  DISABLE REVIEW

    @Test
    void disableReview_ShouldRespondWithClientNotFoundException() {
        when(restTemplate.exchange(anyString(), any(), any(), eq(Client.class)))
                .thenThrow(new ClientNotFoundException("Client not found"));

        // Act
        ResponseEntity<?> response = reviewService.disableReviewByClient(1);

        // Assert
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertEquals("Client not found", response.getBody());
        verify(reviewRepository, never()).findById(anyInt());
    }

    @Test
    void disableReview_whenClientService_ShouldRespondWithBadRequest() {
        // Arrange
        when(restTemplate.exchange(anyString(), any(), any(), eq(Client.class)))
                .thenThrow(new RuntimeException("Error occurred when fetching the client"));

        // Act
        ResponseEntity<?> response = reviewService.disableReviewByClient(1);

        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Failed to fetch the client", response.getBody());
        verify(reviewRepository, never()).findById(anyInt());
    }

    @Test
    void getClient_whenResponseBodyIsEmpty_ShouldRespondWithClientNotFound() {
        // Arrange
        ResponseEntity<Client> emptyResponse = ResponseEntity.ok().build();

        when(restTemplate.exchange(anyString(), any(), any(), eq(Client.class)))
                .thenReturn(emptyResponse);

        // Act & Assert
        assertThrows(RuntimeException.class, () ->
                reviewService.getClient());
    }

    @Test
    void getClient_ShouldRespondWithClientNotFound() {
        // Arrange

        when(restTemplate.exchange(anyString(), any(), any(), eq(Client.class)))
                .thenThrow(new HttpClientErrorException(HttpStatus.NOT_FOUND));

        // Act & Assert
        assertThrows(RuntimeException.class, () -> reviewService.getClient());
    }

    @Test
    void getClient_ShouldRespondWithRuntimeException() {
        // Arrange

        when(restTemplate.exchange(anyString(), any(), any(), eq(Client.class)))
                .thenThrow(new HttpClientErrorException(HttpStatus.SERVICE_UNAVAILABLE));

        // Act & Assert
        assertThrows(ClientServiceException.class, () -> reviewService.getClient());
    }


    @Test
    void disableReview_WhenClientDidNotBorrowItem_ShouldRespondWithBadRequest() {
        int clientId = 1;
        int otherClientId = 2;
        // Arrange
        Client mockClient = new Client("John Doe", "johndoe@gmail.com");
        mockClient.setClientId(clientId);
        Review mockReview = new Review(1, otherClientId, 1,
                "Great book!", 10, false,
                LocalDateTime.now(), LocalDateTime.now(), true);

        when(restTemplate.exchange(anyString(), any(), any(), eq(Client.class)))
                .thenReturn(new ResponseEntity<>(mockClient, HttpStatus.OK));

        when(reviewRepository.findById(1)).thenReturn(Optional.of(mockReview));

        // Act
        ResponseEntity<?> response = reviewService.disableReviewByClient(1);

        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Cannot delete a review written by other client", response.getBody());
    }

    @Test
    void disableReview_ShouldDisableReview() {
        int clientId = 1;
        // Arrange
        Client mockClient = new Client("John Doe", "johndoe@gmail.com");
        mockClient.setClientId(clientId);
        Review mockReview = new Review(1, clientId, 1,
                "Great book!", 10, false,
                LocalDateTime.now(), LocalDateTime.now(), true);

        when(restTemplate.exchange(anyString(), any(), any(), eq(Client.class)))
                .thenReturn(new ResponseEntity<>(mockClient, HttpStatus.OK));

        when(reviewRepository.findById(1)).thenReturn(Optional.of(mockReview));

        // Act
        ResponseEntity<?> response = reviewService.disableReviewByClient(1);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Review disabled successfully", response.getBody());
    }

    @Test
    void getItem_whenResponseBodyIsEmpty_shouldReturnRuntimeException() {
        int itemId = 1;
        ResponseEntity<LibraryItemDTO> emptyResponse = ResponseEntity.ok().build();

        when(restTemplate.exchange(anyString(), any(), any(), eq(LibraryItemDTO.class), eq(itemId)))
                .thenReturn(emptyResponse);

        assertThrows(RuntimeException.class, () -> reviewService.getItem(itemId));
    }

    @Test
    void getItem_shouldReturnItemNotFoundException() {
        int itemId = 1;

        when(restTemplate.exchange(anyString(), any(), any(), eq(LibraryItemDTO.class), eq(itemId)))
                .thenThrow(new HttpClientErrorException(HttpStatus.NOT_FOUND));

        assertThrows(ItemNotFoundException.class, () -> reviewService.getItem(itemId));
    }

    @Test
    void getItem_shouldReturnItemServiceException() {
        int itemId = 1;

        when(restTemplate.exchange(anyString(), any(), any(), eq(LibraryItemDTO.class), eq(1)))
                .thenThrow(new HttpClientErrorException(HttpStatus.SERVICE_UNAVAILABLE));

        assertThrows(ItemServiceException.class, () -> reviewService.getItem(itemId));
    }

}