package com.geodata.repository;

import com.geodata.enums.UserRole;
import com.geodata.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.*;

@Repository
public interface UserRepository extends JpaRepository<User, Integer> {

    Optional<User> findByUsername(String username);

    List<User> findAllByIsEnabledTrue();

    boolean existsByUsernameContaining(String substring);

    Page<User> findByIsEnabledTrueAndNameContainingIgnoreCaseOrIsEnabledTrueAndUsernameContainingIgnoreCase(
            String name,
            String username,
            Pageable pageable
    );
    @Query("""
    SELECT u FROM User u
    WHERE u.isEnabled = true
      AND u.userId <> :excludedId
      AND u.role = :role
      AND (LOWER(u.name) LIKE LOWER(CONCAT('%', :search, '%'))
           OR LOWER(u.username) LIKE LOWER(CONCAT('%', :search, '%')))
    """)
    Page<User> searchEnabledUsersExcludeId(
            @Param("excludedId") Integer excludedId,
            @Param("search") String search,
            @Param("role") UserRole role,
            Pageable pageable
    );


    Page<User> findByIsEnabledTrueAndUserIdNotAndRole(Integer userId, UserRole role, Pageable pageable);
}
