package com.geodata.repository;

import com.geodata.enums.UserRole;
import com.geodata.model.User;
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
import org.springframework.test.annotation.DirtiesContext;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(FlywayAutoConfiguration.class)
class UserRepositoryTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

    @Autowired
    UserRepository userRepository;

    private User user1;
    private User user2;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();

        user1 = userRepository.save(User.builder()
                .username("alice@test.com").name("Alice Smith")
                .password("pass").role(UserRole.USER)
                .isEnabled(true).createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now())
                .build());

        user2 = userRepository.save(User.builder()
                .username("bob@test.com").name("Bob Jones")
                .password("pass").role(UserRole.USER)
                .isEnabled(false).createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now())
                .build());
    }

    @Test
    void findByUsername_existingUser_returnsUser() {
        Optional<User> result = userRepository.findByUsername("alice@test.com");
        assertThat(result).isPresent();
        assertThat(result.get().getName()).isEqualTo("Alice Smith");
    }

    @Test
    void findByUsername_unknownUser_returnsEmpty() {
        assertThat(userRepository.findByUsername("ghost@test.com")).isEmpty();
    }

    @Test
    void findAllByIsEnabledTrue_returnsOnlyEnabled() {
        var enabled = userRepository.findAllByIsEnabledTrue();
        assertThat(enabled).hasSize(1);
        assertThat(enabled.get(0).getUsername()).isEqualTo("alice@test.com");
    }

    @Test
    void findByIsEnabledTrueAndNameContaining_matchesName() {
        Page<User> page = userRepository
                .findByIsEnabledTrueAndNameContainingIgnoreCaseOrIsEnabledTrueAndUsernameContainingIgnoreCase(
                        "alice", "alice", PageRequest.of(0, 10));

        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getContent().get(0).getName()).isEqualTo("Alice Smith");
    }

    @Test
    void findByIsEnabledTrueAndNameContaining_noMatch_returnsEmpty() {
        Page<User> page = userRepository
                .findByIsEnabledTrueAndNameContainingIgnoreCaseOrIsEnabledTrueAndUsernameContainingIgnoreCase(
                        "xyz", "xyz", PageRequest.of(0, 10));

        assertThat(page.getContent()).isEmpty();
    }

    @Test
    void searchEnabledUsersExcludeId_excludesRequester() {
        Page<User> page = userRepository.searchEnabledUsersExcludeId(
                user1.getUserId(), "alice", UserRole.USER, PageRequest.of(0, 10));

        assertThat(page.getContent()).isEmpty();
    }

    @Test
    void findByIsEnabledTrueAndUserIdNotAndRole_excludesUser() {
        User extra = userRepository.save(User.builder()
                .username("carol@test.com").name("Carol")
                .password("pass").role(UserRole.USER)
                .isEnabled(true).createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now())
                .build());

        Page<User> page = userRepository.findByIsEnabledTrueAndUserIdNotAndRole(
                user1.getUserId(), UserRole.USER, PageRequest.of(0, 10));

        assertThat(page.getContent())
                .extracting(User::getUserId)
                .doesNotContain(user1.getUserId());
    }

    @Test
    void existsByUsernameContaining_returnsTrue() {
        assertThat(userRepository.existsByUsernameContaining("alice")).isTrue();
    }

    @Test
    void existsByUsernameContaining_returnsFalse() {
        assertThat(userRepository.existsByUsernameContaining("nobody")).isFalse();
    }
}
