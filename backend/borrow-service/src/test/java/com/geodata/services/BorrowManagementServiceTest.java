package com.geodata.services;

import com.geodata.model.Borrows;
import com.geodata.model.BorrowsStatus;
import com.geodata.repository.BorrowsRepository;
import com.geodata.security.jwt.JwtUtils;
import com.geodata.utils.LibrarianReason;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BorrowManagementServiceTest {

    @Mock private BorrowsRepository borrowsRepository;
    @Mock private JwtUtils jwtUtils;
    @Mock private RestTemplate restTemplate;
    @Mock private HttpServletRequest httpRequest;

    @InjectMocks private BorrowManagementService borrowManagementService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(borrowManagementService, "clientService", "localhost:8010");
        ReflectionTestUtils.setField(borrowManagementService, "mapService", "localhost:8020");
    }

    // ── getReasonMessage ─────────────────────────────────────────────────────

    @Test
    void getReasonMessage_withReason_returnsReason() {
        LibrarianReason reason = new LibrarianReason("Damaged");
        String result = borrowManagementService.getReasonMessage(reason);
        assertThat(result).isEqualTo("Damaged");
    }

    @Test
    void getReasonMessage_nullReason_returnsDefault() {
        String result = borrowManagementService.getReasonMessage(null);
        assertThat(result).isEqualTo("No reason provided");
    }

    @Test
    void getReasonMessage_reasonWithNullText_returnsDefault() {
        LibrarianReason reason = new LibrarianReason(null);
        String result = borrowManagementService.getReasonMessage(reason);
        assertThat(result).isEqualTo("No reason provided");
    }

    // ── getBorrowRequestsNumber ───────────────────────────────────────────────

    @Test
    void getBorrowRequestsNumber_returnsCount() {
        Borrows b = Borrows.builder().id(1).status(BorrowsStatus.BORROW_REQUESTED.dbValue()).build();
        when(borrowsRepository.findAllByStatus(BorrowsStatus.BORROW_REQUESTED.dbValue()))
                .thenReturn(List.of(b));

        Integer count = borrowManagementService.getBorrowRequestsNumber();

        assertThat(count).isEqualTo(1);
    }

    @Test
    void getBorrowRequestsNumber_noRequests_returnsZero() {
        when(borrowsRepository.findAllByStatus(BorrowsStatus.BORROW_REQUESTED.dbValue()))
                .thenReturn(List.of());

        Integer count = borrowManagementService.getBorrowRequestsNumber();

        assertThat(count).isZero();
    }

    // ── getReturnRequestsNumber ───────────────────────────────────────────────

    @Test
    void getReturnRequestsNumber_returnsCount() {
        Borrows b = Borrows.builder().id(1).status(BorrowsStatus.RETURN_REQUESTED.dbValue()).build();
        when(borrowsRepository.findAllByStatus(BorrowsStatus.RETURN_REQUESTED.dbValue()))
                .thenReturn(List.of(b));

        Integer count = borrowManagementService.getReturnRequestsNumber();

        assertThat(count).isEqualTo(1);
    }
}
