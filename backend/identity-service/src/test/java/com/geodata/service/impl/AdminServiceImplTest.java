package com.geodata.service.impl;

import com.geodata.enums.UserRole;
import com.geodata.exceptions.UserNotFoundException;
import com.geodata.model.User;
import com.geodata.repository.UserRepository;
import com.geodata.utils.ChangeRoleRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminServiceImplTest {

    @Mock private UserRepository userRepository;
    @InjectMocks private AdminServiceImpl adminService;

    private User user;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .userId(1)
                .username("marius@test.com")
                .name("Marius")
                .role(UserRole.USER)
                .isEnabled(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    @Test
    void getAllUsers_returnsAllUsers() {
        when(userRepository.findAll()).thenReturn(List.of(user));

        List<User> result = adminService.getAllUsers();

        assertThat(result).hasSize(1).contains(user);
    }

    @Test
    void getAllEnableUsers_returnsOnlyEnabledUsers() {
        when(userRepository.findAllByIsEnabledTrue()).thenReturn(List.of(user));

        List<User> result = adminService.getAllEnableUsers();

        assertThat(result).hasSize(1);
    }

    @Test
    void disableUserByEmail_success() {
        when(userRepository.findByUsername("marius@test.com")).thenReturn(Optional.of(user));

        String result = adminService.disableUserByEmail("marius@test.com");

        assertThat(result).contains("Disabled successfully");
        verify(userRepository).save(argThat(u -> !u.isEnabled()));
    }

    @Test
    void disableUserByEmail_userNotFound_throws() {
        when(userRepository.findByUsername("missing@test.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adminService.disableUserByEmail("missing@test.com"))
                .isInstanceOf(UserNotFoundException.class);
    }

    @Test
    void changeRoleToAnUser_success() {
        when(userRepository.findByUsername("marius@test.com")).thenReturn(Optional.of(user));

        ResponseEntity<String> resp = adminService.changeRoleToAnUser(
                new ChangeRoleRequest("marius@test.com", "ADMIN"));

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(userRepository).save(argThat(u -> u.getRole() == UserRole.ADMIN));
    }

    @Test
    void changeRoleToAnUser_userNotFound_throws() {
        when(userRepository.findByUsername("ghost@test.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                adminService.changeRoleToAnUser(new ChangeRoleRequest("ghost@test.com", "USER")))
                .isInstanceOf(UserNotFoundException.class);
    }
}
