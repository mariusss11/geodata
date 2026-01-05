package com.geodata.repository;

import com.geodata.model.Borrows;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface BorrowsRepository extends JpaRepository<Borrows, Integer> {

    long deleteByItemId(int itemId);

    void deleteByClientId(int clientId);

    List<Borrows> findByUserIdAndMapIdAndStatus(int userId, int mapId, String status);

    Optional<Borrows> findByClientIdAndItemId(int clientId, int itemId);

    List<Borrows> findAllByClientId(int clientId);

    /**
     * Retrieves the borrow history for a specific client based on the provided status.
     * <p>
     * This method is typically used when a client wants to view their past borrow records.
     * Only borrows that match the given status (e.g., "returned") will be included,
     * as only completed borrows are considered part of the borrow history.
     * </p>
     *
     * @param clientId the unique identifier of the client whose borrow history is being requested
     * @param status the status used to filter borrows (e.g., only "returned" borrows)
     * @return a list of borrow records that match the specified status for the given client
     */

    List<Borrows> findAllByClientIdAndStatus(int clientId, String status);

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

    List<Borrows> findBorrowItemsByClientIdAndStatus(int clientId, String status);


    List<Borrows> findAllByStatus(String s);

    @Query(value = """
            SELECT * FROM borrows\s
                WHERE user_id = :userId
                AND status = 'borrowed'
                AND map_id IN (
                    SELECT item_id FROM maps\s
                    WHERE maps_id IN (:mapIds)
                )
            """, nativeQuery = true)
    List<Borrows> findBorrowsCurrentlyBorrowedByClientFiltered(
            @Param("userId") int userId,
            @Param("mapIds") List<Integer> mapIds
    );
}
