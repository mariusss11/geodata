package com.geodata.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.geodata.dto.ChangePasswordRequest;
import com.geodata.dto.Response;
import com.geodata.dto.UpdateProfileRequest;
import com.geodata.enums.UserRole;
import com.geodata.model.User;
import com.geodata.security.CustomUserDetailsService;
import com.geodata.security.configuration.UserServiceSecurityConfig;
import com.geodata.security.jwt.JwtUtils;
import com.geodata.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(HomeController.class)
@Import(UserServiceSecurityConfig.class)
class HomeControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @MockBean UserService userService;
    @MockBean JwtUtils jwtUtils;
    @MockBean CustomUserDetailsService customUserDetailsService;

    private User sampleUser() {
        return User.builder()
                .userId(1).username("alice@test.com").name("Alice")
                .role(UserRole.USER).isEnabled(true)
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now())
                .build();
    }

    @Test
    @WithMockUser(authorities = "USER")
    void getUserInfo_authenticated_returns200() throws Exception {
        when(userService.getUserInfo()).thenReturn(sampleUser());

        mockMvc.perform(get("/api/home"))
                .andExpect(status().isOk());
    }

    @Test
    void getUserInfo_unauthenticated_returns403() throws Exception {
        // Spring Security 6 stateless: anonymous users hit denyAll fallback → 403
        mockMvc.perform(get("/api/home"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "USER")
    void whoami_deniedBySecurityConfig_returns403() throws Exception {
        // /api/home/whoami has no explicit rule → falls to anyRequest().denyAll()
        // Phase 3 should widen the /api/home rule to /api/home/**
        mockMvc.perform(get("/api/home/whoami"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "USER")
    void updateProfile_validRequest_returns200() throws Exception {
        when(userService.updateProfile(any())).thenReturn(sampleUser());

        mockMvc.perform(put("/api/home/profile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdateProfileRequest("New Name"))))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(authorities = "USER")
    void changePassword_validRequest_returns204() throws Exception {
        mockMvc.perform(put("/api/home/password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new ChangePasswordRequest("oldPass", "newPass"))))
                .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(authorities = "USER")
    void disableUser_returns200() throws Exception {
        when(userService.disableUser(any())).thenReturn("Disabled successfully");

        mockMvc.perform(put("/api/home/disable"))
                .andExpect(status().isOk());
    }
}
