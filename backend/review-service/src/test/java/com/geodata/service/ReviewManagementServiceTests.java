package com.geodata.service;

import com.geodata.security.jwt.JwtUtils;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ReviewManagementServiceTests {

    @InjectMocks
    private ReviewManagementService reviewManagementService;

    @Mock
    private ReviewService reviewService;

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
    void disableReviewByAdmin_withValidReviewId_shouldReturnResponseEntityOk() {
        // Arrange
        int reviewId = 1;

        doNothing().when(reviewService).disableReview(reviewId);

        // Act
        ResponseEntity<?> response = reviewManagementService.disableReviewByAdmin(reviewId);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Review disabled successfully", response.getBody());
    }
}