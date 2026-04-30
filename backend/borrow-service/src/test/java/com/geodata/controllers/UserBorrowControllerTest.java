package com.geodata.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.geodata.model.PagedResponse;
import com.geodata.security.config.BorrowServiceSecurityConfig;
import com.geodata.security.jwt.JwtUtils;
import com.geodata.services.BorrowsService;
import com.geodata.utils.BorrowedMapDto;
import com.geodata.utils.TransferMapRequest;
import com.geodata.utils.TransferResponse;
import com.geodata.utils.requests.BorrowMapRequest;
import com.geodata.utils.requests.ReturnMapRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UserBorrowController.class)
@Import(BorrowServiceSecurityConfig.class)
class UserBorrowControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @MockBean BorrowsService borrowsService;
    @MockBean JwtUtils jwtUtils;

    @Test
    @WithMockUser(authorities = "USER")
    void getClientBorrows_returns200() throws Exception {
        when(borrowsService.getClientBorrowedItems(1)).thenReturn(List.of(10, 20));

        mockMvc.perform(get("/api/borrows/1"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(authorities = "USER")
    void createBorrow_returns200() throws Exception {
        when(borrowsService.borrowMap(any())).thenReturn(ResponseEntity.ok("Borrow started successfully"));

        mockMvc.perform(post("/api/borrows/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new BorrowMapRequest(1, "Alice", LocalDate.now().plusDays(7)))))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(authorities = "USER")
    void returnBorrow_returns200() throws Exception {
        when(borrowsService.returnMap(any())).thenReturn(ResponseEntity.ok("Map successfully returned"));

        mockMvc.perform(post("/api/borrows/return")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ReturnMapRequest(10))))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(authorities = "USER")
    void transferBorrow_returns200() throws Exception {
        TransferResponse transferResp = TransferResponse.builder()
                .newBorrowerName("Bob").build();
        when(borrowsService.transferBorrowedItem(any())).thenReturn(transferResp);

        mockMvc.perform(post("/api/borrows/transfer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new TransferMapRequest(10, "Bob", LocalDate.now().plusDays(7)))))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(authorities = "USER")
    void hasBorrowed_returns200() throws Exception {
        when(borrowsService.hadBorrowedItem(1, 10)).thenReturn(true);

        mockMvc.perform(get("/api/borrows/hasBorrowed/1/10"))
                .andExpect(status().isOk())
                .andExpect(content().string("true"));
    }

    @Test
    @WithMockUser(authorities = "USER")
    void getCurrentBorrows_returns200() throws Exception {
        PagedResponse<BorrowedMapDto> page = new PagedResponse<>();
        when(borrowsService.getUserCurrentBorrows(eq(0), eq(10), any())).thenReturn(page);

        mockMvc.perform(get("/api/borrows/current")
                        .param("pageNumber", "0")
                        .param("pageSize", "10"))
                .andExpect(status().isOk());
    }

    @Test
    void createBorrow_unauthenticated_returns403() throws Exception {
        mockMvc.perform(post("/api/borrows/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new BorrowMapRequest(1, "Alice", LocalDate.now().plusDays(7)))))
                .andExpect(status().isForbidden());
    }
}
