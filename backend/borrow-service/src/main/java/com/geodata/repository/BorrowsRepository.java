package com.geodata.repository;

import com.geodata.model.Borrows;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.List;
import java.util.Optional;

public interface BorrowsRepository extends JpaRepository<Borrows, Integer> {

    long deleteByMapId(int itemId);

    void deleteByUserId(int userId);

    List<Borrows> findByUserIdAndMapIdAndStatus(int userId, int mapId, String status);

    Optional<Borrows> findByUserIdAndMapId(int userId, int itemId);

    List<Borrows> findAllByUserId(int userId);

    /**
     * Retrieves the borrow history for a specific client based on the provided status.
     * <p>
     * This method is typically used when a client wants to view their past borrow records.
     * Only borrows that match the given status (e.g., "returned") will be included,
     * as only completed borrows are considered part of the borrow history.
     * </p>
     *
     * @param userId the unique identifier of the client whose borrow history is being requested
     * @param status the status used to filter borrows (e.g., only "returned" borrows)
     * @return a list of borrow records that match the specified status for the given client
     */

    List<Borrows> findAllByUserIdAndStatus(int userId, String status);

    /**
     * Method used to get all the history of a client borrow history <br>
     * It also contains all the items ever borrowed
     * @param userId the id which is used to identify the client
     * @return all the rows where this client is
     */
    @Query(value = """
        SELECT m.name, m.year, c.email, COUNT(*)
        FROM users c
        JOIN borrows b ON c.user_id = b.user_id
        JOIN maps m ON m.item_id = b.item_id
        WHERE c.user_id = :userId
        GROUP BY m.name, m.year, c.email;
    """, nativeQuery = true)
    List<Object[]> findMostBorrowedItemsByUser(@Param("userId") int userId);

    List<Borrows> findBorrowMapsByUserIdAndStatus(int userId, String status);

    Borrows findBorrowMapByUserIdAndMapIdAndStatus(int userId, int mapId, String status);


    List<Borrows> findAllByStatus(String s);

    Page<Borrows> findAllByStatus(String status, Pageable pageable);

}
