package com.geodata.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.geodata.security.config.BorrowServiceSecurityConfig;
import com.geodata.security.jwt.JwtUtils;
import com.geodata.services.BorrowManagementService;
import com.geodata.utils.LibrarianReason;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(BorrowManagementController.class)
@Import(BorrowServiceSecurityConfig.class)
class BorrowManagementControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @MockBean BorrowManagementService borrowManagementService;
    @MockBean JwtUtils jwtUtils;

    @Test
    @WithMockUser(authorities = "LIBRARIAN")
    void getBorrowRequestsNumber_returns200() throws Exception {
        when(borrowManagementService.getBorrowRequestsNumber()).thenReturn(3);

        mockMvc.perform(get("/api/borrows/librarian/borrowRequests/number"))
                .andExpect(status().isOk())
                .andExpect(content().string("3"));
    }

    @Test
    @WithMockUser(authorities = "LIBRARIAN")
    void getReturnRequestsNumber_returns200() throws Exception {
        when(borrowManagementService.getReturnRequestsNumber()).thenReturn(1);

        mockMvc.perform(get("/api/borrows/librarian/returnRequests/number"))
                .andExpect(status().isOk())
                .andExpect(content().string("1"));
    }

    @Test
    @WithMockUser(authorities = "LIBRARIAN")
    void approveBorrow_returns200() throws Exception {
        when(borrowManagementService.approveBorrowByLibrarian(eq(1), eq(10), any()))
                .thenReturn(ResponseEntity.ok("Approved"));

        mockMvc.perform(post("/api/borrows/librarian/approveBorrow/1/10")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LibrarianReason("All good"))))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(authorities = "LIBRARIAN")
    void declineBorrow_returns200() throws Exception {
        when(borrowManagementService.declineBorrowByLibrarian(eq(1), eq(10), any()))
                .thenReturn(ResponseEntity.ok("Declined"));

        mockMvc.perform(post("/api/borrows/librarian/declineBorrow/1/10")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LibrarianReason("Overdue"))))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(authorities = "LIBRARIAN")
    void declineReturn_returns200() throws Exception {
        when(borrowManagementService.declineReturnByLibrarian(eq(1), eq(10), any()))
                .thenReturn(ResponseEntity.ok("Return declined"));

        mockMvc.perform(post("/api/borrows/librarian/declineReturn/1/10")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LibrarianReason("Damaged"))))
                .andExpect(status().isOk());
    }

    @Test
    void getBorrowRequestsNumber_unauthenticated_returns403() throws Exception {
        mockMvc.perform(get("/api/borrows/librarian/borrowRequests/number"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "USER")
    void getBorrowRequestsNumber_wrongRole_returns403() throws Exception {
        mockMvc.perform(get("/api/borrows/librarian/borrowRequests/number"))
                .andExpect(status().isForbidden());
    }
}
