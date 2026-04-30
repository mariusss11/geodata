package com.geodata.services;

import com.geodata.exceptions.BorrowNotFoundException;
import com.geodata.exceptions.InvalidMapException;
import com.geodata.exceptions.InvalidReturnDate;
import com.geodata.exceptions.TransferMapException;
import com.geodata.model.Borrows;
import com.geodata.model.BorrowsStatus;
import com.geodata.model.Map;
import com.geodata.repository.BorrowsRepository;
import com.geodata.security.jwt.JwtUtils;
import com.geodata.utils.BorrowStatus;
import com.geodata.utils.TransferResponse;
import com.geodata.utils.requests.BorrowMapRequest;
import com.geodata.utils.requests.ReturnMapRequest;
import com.geodata.utils.TransferMapRequest;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.*;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BorrowsServiceTest {

    @Mock private BorrowsRepository borrowsRepository;
    @Mock private RestTemplate restTemplate;
    @Mock private JwtUtils jwtUtils;
    @Mock private HttpServletRequest httpRequest;

    @InjectMocks private BorrowsService borrowsService;

    private Map availableMap;
    private Map borrowedMap;
    private Borrows activeBorrow;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(borrowsService, "userService", "localhost:8010");
        ReflectionTestUtils.setField(borrowsService, "mapService", "localhost:8020");

        availableMap = new Map();
        availableMap.setId(1);
        availableMap.setName("Topo Map");
        availableMap.setYear(2020);
        availableMap.setAvailabilityStatus("AVAILABLE");
        availableMap.setEnabled(true);
        availableMap.setCreatedAt(LocalDateTime.now());
        availableMap.setUpdatedAt(LocalDateTime.now());

        borrowedMap = new Map();
        borrowedMap.setId(1);
        borrowedMap.setName("Topo Map");
        borrowedMap.setYear(2020);
        borrowedMap.setAvailabilityStatus("BORROWED");
        borrowedMap.setEnabled(true);
        borrowedMap.setCreatedAt(LocalDateTime.now());
        borrowedMap.setUpdatedAt(LocalDateTime.now());

        activeBorrow = Borrows.builder()
                .id(10)
                .mapId(1)
                .borrowerName("Alice")
                .borrowDate(LocalDateTime.now())
                .returnDate(LocalDate.now().plusDays(7))
                .status(BorrowsStatus.BORROWED.dbValue())
                .build();
    }

    // ── borrowMap ────────────────────────────────────────────────────────────

    @Test
    void borrowMap_success() {
        stubAuthToken();
        stubMapFetch(availableMap);
        stubLockMap(HttpStatus.OK);
        when(borrowsRepository.save(any())).thenReturn(activeBorrow);

        BorrowMapRequest req = new BorrowMapRequest(1, "Alice", LocalDate.now().plusDays(5));
        ResponseEntity<String> resp = borrowsService.borrowMap(req);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void borrowMap_pastReturnDate_throwsInvalidReturnDate() {
        BorrowMapRequest req = new BorrowMapRequest(1, "Alice", LocalDate.now().minusDays(1));

        assertThatThrownBy(() -> borrowsService.borrowMap(req))
                .isInstanceOf(InvalidReturnDate.class);
        verifyNoInteractions(restTemplate);
    }

    @Test
    void borrowMap_mapNotAvailable_returnsBadRequest() {
        stubAuthToken();
        stubMapFetch(borrowedMap);

        BorrowMapRequest req = new BorrowMapRequest(1, "Alice", LocalDate.now().plusDays(5));
        ResponseEntity<String> resp = borrowsService.borrowMap(req);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    // ── returnMap ────────────────────────────────────────────────────────────

    @Test
    void returnMap_success() {
        stubAuthToken();
        when(borrowsRepository.findById(10)).thenReturn(Optional.of(activeBorrow));
        stubUnlockMap(HttpStatus.OK);
        when(borrowsRepository.save(any())).thenReturn(activeBorrow);

        ResponseEntity<String> resp = borrowsService.returnMap(new ReturnMapRequest(10));

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(borrowsRepository).save(argThat(b -> b.getStatus().equalsIgnoreCase("RETURNED")));
    }

    @Test
    void returnMap_notBorrowedStatus_returnsBadRequest() {
        activeBorrow.setStatus("RETURNED");
        when(borrowsRepository.findById(10)).thenReturn(Optional.of(activeBorrow));
        stubAuthToken();

        ResponseEntity<String> resp = borrowsService.returnMap(new ReturnMapRequest(10));

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void returnMap_borrowNotFound_throws() {
        when(borrowsRepository.findById(99)).thenReturn(Optional.empty());
        stubAuthToken();

        assertThatThrownBy(() -> borrowsService.returnMap(new ReturnMapRequest(99)))
                .isInstanceOf(BorrowNotFoundException.class);
    }

    // ── transferBorrowedItem ──────────────────────────────────────────────────

    @Test
    void transferBorrowedItem_success() {
        stubAuthToken();
        when(borrowsRepository.findById(10)).thenReturn(Optional.of(activeBorrow));
        // map that CAN be transferred: available=true, enabled=true
        Map transferableMap = new Map();
        transferableMap.setId(1);
        transferableMap.setAvailabilityStatus("AVAILABLE");
        transferableMap.setEnabled(true);
        transferableMap.setCreatedAt(LocalDateTime.now());
        transferableMap.setUpdatedAt(LocalDateTime.now());
        stubMapFetch(transferableMap);
        when(borrowsRepository.save(any())).thenReturn(activeBorrow);

        TransferMapRequest req = new TransferMapRequest(10, "Bob", LocalDate.now().plusDays(10));
        TransferResponse resp = borrowsService.transferBorrowedItem(req);

        assertThat(resp.getNewBorrowerName()).isEqualTo("Bob");
        verify(borrowsRepository, times(2)).save(any(Borrows.class));
    }

    @Test
    void transferBorrowedItem_notBorrowedStatus_throws() {
        activeBorrow.setStatus("RETURNED");
        when(borrowsRepository.findById(10)).thenReturn(Optional.of(activeBorrow));

        // status check happens before getAuthToken() — no auth stub needed
        assertThatThrownBy(() ->
                borrowsService.transferBorrowedItem(new TransferMapRequest(10, "Bob", LocalDate.now().plusDays(5))))
                .isInstanceOf(TransferMapException.class);
    }

    @Test
    void transferBorrowedItem_mapNotTransferable_throws() {
        stubAuthToken();
        when(borrowsRepository.findById(10)).thenReturn(Optional.of(activeBorrow));
        // map that CANNOT be transferred: it's borrowed (not available)
        stubMapFetch(borrowedMap);

        assertThatThrownBy(() ->
                borrowsService.transferBorrowedItem(new TransferMapRequest(10, "Bob", LocalDate.now().plusDays(5))))
                .isInstanceOf(InvalidMapException.class);
    }

    @Test
    void transferBorrowedItem_notFound_throws() {
        when(borrowsRepository.findById(99)).thenReturn(Optional.empty());

        // getBorrowById throws before getAuthToken() is called — no auth stub needed
        assertThatThrownBy(() ->
                borrowsService.transferBorrowedItem(new TransferMapRequest(99, "Bob", LocalDate.now().plusDays(5))))
                .isInstanceOf(BorrowNotFoundException.class);
    }

    // ── getClientBorrowedItems ────────────────────────────────────────────────

    @Test
    void getClientBorrowedItems_returnsMapIds() {
        when(borrowsRepository.findAllByUserIdAndStatus(1, BorrowsStatus.RETURNED.dbValue()))
                .thenReturn(List.of(activeBorrow));

        List<Integer> result = borrowsService.getClientBorrowedItems(1);

        assertThat(result).contains(1);
    }

    // ── hadBorrowedItem ───────────────────────────────────────────────────────

    @Test
    void hadBorrowedItem_whenRecordExists_returnsTrue() {
        when(borrowsRepository.findByUserIdAndMapIdAndStatus(1, 1, BorrowsStatus.RETURNED.dbValue()))
                .thenReturn(List.of(activeBorrow));

        assertThat(borrowsService.hadBorrowedItem(1, 1)).isTrue();
    }

    @Test
    void hadBorrowedItem_whenNoRecord_returnsFalse() {
        when(borrowsRepository.findByUserIdAndMapIdAndStatus(1, 2, BorrowsStatus.RETURNED.dbValue()))
                .thenReturn(List.of());

        assertThat(borrowsService.hadBorrowedItem(1, 2)).isFalse();
    }

    // ── removeItemByIdFromBorrowsList ─────────────────────────────────────────

    @Test
    void removeItemByIdFromBorrowsList_returnsDeletedCount() {
        when(borrowsRepository.deleteByMapId(1)).thenReturn(3L);

        long deleted = borrowsService.removeItemByIdFromBorrowsList(1);

        assertThat(deleted).isEqualTo(3L);
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private void stubAuthToken() {
        when(httpRequest.getHeader("Authorization")).thenReturn("Bearer test-token");
        when(jwtUtils.getToken("Bearer test-token")).thenReturn("test-token");
    }

    @SuppressWarnings("unchecked")
    private void stubMapFetch(Map map) {
        ResponseEntity<Map> mapResp = new ResponseEntity<>(map, HttpStatus.OK);
        when(restTemplate.exchange(
                contains("/api/maps/"),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                eq(Map.class),
                anyInt()))
                .thenReturn(mapResp);
    }

    private void stubLockMap(HttpStatus status) {
        when(restTemplate.exchange(
                contains("setAvailableToBorrowed"),
                eq(HttpMethod.PUT),
                any(HttpEntity.class),
                eq(Void.class)))
                .thenReturn(new ResponseEntity<>(status));
    }

    private void stubUnlockMap(HttpStatus status) {
        when(restTemplate.exchange(
                contains("setBorrowedToAvailable"),
                eq(HttpMethod.PUT),
                any(HttpEntity.class),
                eq(Void.class)))
                .thenReturn(new ResponseEntity<>(status));
    }
}
