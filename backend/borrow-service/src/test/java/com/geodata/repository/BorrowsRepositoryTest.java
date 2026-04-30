package com.geodata.repository;

import com.geodata.model.Borrows;
import com.geodata.model.BorrowsStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(FlywayAutoConfiguration.class)
class BorrowsRepositoryTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

    @Autowired
    BorrowsRepository borrowsRepository;

    private Borrows activeBorrow;
    private Borrows returnedBorrow;

    @BeforeEach
    void setUp() {
        borrowsRepository.deleteAll();

        activeBorrow = borrowsRepository.save(Borrows.builder()
                .userId(1).mapId(10)
                .borrowerName("Alice")
                .borrowDate(LocalDateTime.now())
                .returnDate(LocalDate.now().plusDays(7))
                .status(BorrowsStatus.BORROWED.dbValue())
                .build());

        returnedBorrow = borrowsRepository.save(Borrows.builder()
                .userId(1).mapId(20)
                .borrowerName("Alice")
                .borrowDate(LocalDateTime.now().minusDays(14))
                .returnDate(LocalDate.now().minusDays(7))
                .actualReturnDate(LocalDateTime.now().minusDays(8))
                .status(BorrowsStatus.RETURNED.dbValue())
                .build());
    }

    @Test
    void findAllByStatus_borrowed_returnsActiveBorrows() {
        List<Borrows> result = borrowsRepository.findAllByStatus(BorrowsStatus.BORROWED.dbValue());
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getBorrowerName()).isEqualTo("Alice");
    }

    @Test
    void findAllByStatus_returned_returnsReturnedBorrows() {
        List<Borrows> result = borrowsRepository.findAllByStatus(BorrowsStatus.RETURNED.dbValue());
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getMapId()).isEqualTo(20);
    }

    @Test
    void findAllByStatus_paginated_returnsPage() {
        Page<Borrows> page = borrowsRepository.findAllByStatus(
                BorrowsStatus.BORROWED.dbValue(), PageRequest.of(0, 10));
        assertThat(page.getContent()).hasSize(1);
    }

    @Test
    void findByUserIdAndMapIdAndStatus_matchingRecord_returnsList() {
        List<Borrows> result = borrowsRepository.findByUserIdAndMapIdAndStatus(
                1, 10, BorrowsStatus.BORROWED.dbValue());
        assertThat(result).hasSize(1);
    }

    @Test
    void findByUserIdAndMapIdAndStatus_noMatch_returnsEmpty() {
        List<Borrows> result = borrowsRepository.findByUserIdAndMapIdAndStatus(
                99, 99, BorrowsStatus.BORROWED.dbValue());
        assertThat(result).isEmpty();
    }

    @Test
    void findAllByUserIdAndStatus_returnsUserBorrows() {
        List<Borrows> result = borrowsRepository.findAllByUserIdAndStatus(1, BorrowsStatus.RETURNED.dbValue());
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getMapId()).isEqualTo(20);
    }

    @Test
    void deleteByMapId_deletesAllBorrowsForMap() {
        long deleted = borrowsRepository.deleteByMapId(10);
        assertThat(deleted).isEqualTo(1L);
        assertThat(borrowsRepository.findAllByStatus(BorrowsStatus.BORROWED.dbValue())).isEmpty();
    }

    @Test
    void findBorrowMapsByUserIdAndStatus_returnsMatchingBorrows() {
        List<Borrows> result = borrowsRepository.findBorrowMapsByUserIdAndStatus(
                1, BorrowsStatus.BORROWED.dbValue());
        assertThat(result).hasSize(1);
    }

    @Test
    void findAllByUserId_returnsAllUserBorrows() {
        List<Borrows> result = borrowsRepository.findAllByUserId(1);
        assertThat(result).hasSize(2);
    }
}
