package com.geodata.service.impl;

import com.geodata.dto.ChangePasswordRequest;
import com.geodata.dto.CreateUserRequest;
import com.geodata.dto.LoginRequest;
import com.geodata.dto.UpdateProfileRequest;
import com.geodata.enums.UserRole;
import com.geodata.exceptions.InvalidPasswordException;
import com.geodata.exceptions.UserNotFoundException;
import com.geodata.model.User;
import com.geodata.repository.UserRepository;
import com.geodata.security.jwt.JwtUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.atLeastOnce;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtUtils jwtUtils;
    @Mock private RestTemplate restTemplate;

    @InjectMocks private UserServiceImpl userService;

    private User sampleUser;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(userService, "borrowService", "localhost:8030");
        sampleUser = User.builder()
                .userId(1)
                .username("marius@test.com")
                .name("Marius")
                .password("encoded-pass")
                .role(UserRole.USER)
                .isEnabled(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    // ── signUp ──────────────────────────────────────────────────────────────

    @Test
    void signUp_success() {
        when(userRepository.findByUsername("marius@test.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("pass")).thenReturn("encoded");
        when(userRepository.save(any(User.class))).thenReturn(sampleUser);

        ResponseEntity<String> resp = userService.signUp(
                new CreateUserRequest("marius@test.com", "Marius", "pass"));

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody()).contains("successfully");
    }

    @Test
    void signUp_duplicateUsername_returns400() {
        when(userRepository.findByUsername("marius@test.com")).thenReturn(Optional.of(sampleUser));

        ResponseEntity<String> resp = userService.signUp(
                new CreateUserRequest("marius@test.com", "Marius", "pass"));

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        verify(userRepository, never()).save(any());
    }

    @Test
    void signUp_withExplicitRole_assignsRole() {
        when(userRepository.findByUsername(any())).thenReturn(Optional.empty());
        when(passwordEncoder.encode(any())).thenReturn("enc");
        when(userRepository.save(any())).thenReturn(sampleUser);

        userService.signUp(new CreateUserRequest("admin@test.com", "Admin", "pass", "ADMIN"));

        verify(userRepository, atLeastOnce()).save(argThat(u -> u.getRole() == UserRole.ADMIN));
    }

    // ── login ────────────────────────────────────────────────────────────────

    @Test
    void login_success() {
        when(userRepository.findByUsername("marius@test.com")).thenReturn(Optional.of(sampleUser));
        when(passwordEncoder.matches("pass", "encoded-pass")).thenReturn(true);
        when(jwtUtils.generateToken("marius@test.com")).thenReturn("jwt-token");

        ResponseEntity<?> resp = userService.login(new LoginRequest("marius@test.com", "pass"));

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void login_userNotFound_returns404() {
        when(userRepository.findByUsername(any())).thenReturn(Optional.empty());

        ResponseEntity<?> resp = userService.login(new LoginRequest("unknown@test.com", "pass"));

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void login_disabledUser_returns401() {
        sampleUser.setEnabled(false);
        when(userRepository.findByUsername("marius@test.com")).thenReturn(Optional.of(sampleUser));

        ResponseEntity<?> resp = userService.login(new LoginRequest("marius@test.com", "pass"));

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void login_wrongPassword_returns401() {
        when(userRepository.findByUsername("marius@test.com")).thenReturn(Optional.of(sampleUser));
        when(passwordEncoder.matches("wrong", "encoded-pass")).thenReturn(false);

        ResponseEntity<?> resp = userService.login(new LoginRequest("marius@test.com", "wrong"));

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    // ── changePassword ───────────────────────────────────────────────────────

    @Test
    void changePassword_success() {
        mockSecurityContext("marius@test.com");
        when(userRepository.findByUsername("marius@test.com")).thenReturn(Optional.of(sampleUser));
        when(passwordEncoder.matches("old", "encoded-pass")).thenReturn(true);
        when(passwordEncoder.encode("new")).thenReturn("new-encoded");

        userService.changePassword(new ChangePasswordRequest("old", "new"));

        verify(userRepository).save(argThat(u -> u.getPassword().equals("new-encoded")));
    }

    @Test
    void changePassword_wrongCurrentPassword_throws() {
        mockSecurityContext("marius@test.com");
        when(userRepository.findByUsername("marius@test.com")).thenReturn(Optional.of(sampleUser));
        when(passwordEncoder.matches("wrong", "encoded-pass")).thenReturn(false);

        assertThatThrownBy(() -> userService.changePassword(new ChangePasswordRequest("wrong", "new")))
                .isInstanceOf(InvalidPasswordException.class);
    }

    // ── updateProfile ────────────────────────────────────────────────────────

    @Test
    void updateProfile_updatesNameAndTimestamp() {
        mockSecurityContext("marius@test.com");
        when(userRepository.findByUsername("marius@test.com")).thenReturn(Optional.of(sampleUser));
        when(userRepository.save(any())).thenReturn(sampleUser);

        User result = userService.updateProfile(new UpdateProfileRequest("New Name"));

        verify(userRepository).save(argThat(u -> u.getName().equals("New Name")));
    }

    // ── getUsersPaginated ────────────────────────────────────────────────────

    @Test
    void getUsersPaginated_noSearch_returnsAll() {
        Page<User> page = new PageImpl<>(List.of(sampleUser));
        when(userRepository.findAll(any(Pageable.class))).thenReturn(page);

        var result = userService.getUsersPaginated(0, 10, null);

        assertThat(result.getContent()).hasSize(1);
    }

    @Test
    void getUsersPaginated_withSearch_filtersResults() {
        Page<User> page = new PageImpl<>(List.of(sampleUser));
        when(userRepository
                .findByIsEnabledTrueAndNameContainingIgnoreCaseOrIsEnabledTrueAndUsernameContainingIgnoreCase(
                        eq("Mar"), eq("Mar"), any(Pageable.class)))
                .thenReturn(page);

        var result = userService.getUsersPaginated(0, 10, "Mar");

        assertThat(result.getContent()).hasSize(1);
    }

    // ── disableUser ──────────────────────────────────────────────────────────

    @Test
    void disableUser_success() {
        when(userRepository.findByUsername("marius@test.com")).thenReturn(Optional.of(sampleUser));

        String result = userService.disableUser("marius@test.com");

        assertThat(result).contains("Disabled successfully");
        verify(userRepository).save(argThat(u -> !u.isEnabled()));
    }

    @Test
    void disableUser_notFound_throws() {
        when(userRepository.findByUsername("missing@test.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.disableUser("missing@test.com"))
                .isInstanceOf(UserNotFoundException.class);
    }

    // ── getUserById ──────────────────────────────────────────────────────────

    @Test
    void getUserById_returnsUser() {
        when(userRepository.getReferenceById(1)).thenReturn(sampleUser);

        User result = userService.getUserById(1);

        assertThat(result).isEqualTo(sampleUser);
    }

    // ── getCurrentLoggedInUser ───────────────────────────────────────────────

    @Test
    void getCurrentLoggedInUser_notFound_throws() {
        mockSecurityContext("ghost@test.com");
        when(userRepository.findByUsername("ghost@test.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getCurrentLoggedInUser())
                .isInstanceOf(UserNotFoundException.class);
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private void mockSecurityContext(String username) {
        Authentication auth = mock(Authentication.class);
        when(auth.getName()).thenReturn(username);
        SecurityContext ctx = mock(SecurityContext.class);
        when(ctx.getAuthentication()).thenReturn(auth);
        SecurityContextHolder.setContext(ctx);
    }
}
