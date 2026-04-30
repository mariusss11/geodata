package com.geodata.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.geodata.model.Map;
import com.geodata.model.PagedResponse;
import com.geodata.security.config.MapServiceSecurityConfig;
import com.geodata.security.jwt.JwtUtils;
import com.geodata.service.MapService;
import com.geodata.dto.MapsStats;
import com.geodata.utils.GetItemRequest;
import com.geodata.utils.LockItemRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(MapsController.class)
@Import(MapServiceSecurityConfig.class)
class MapsControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @MockBean MapService mapService;
    @MockBean JwtUtils jwtUtils;

    private Map sampleMap() {
        return Map.builder()
                .id(1).name("Topographic Map").year(2020)
                .availabilityStatus("AVAILABLE").isEnabled(true)
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now())
                .build();
    }

    @Test
    @WithMockUser(authorities = "USER")
    void getMapById_returns200() throws Exception {
        when(mapService.getMap(1)).thenReturn(sampleMap());

        mockMvc.perform(get("/api/maps/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Topographic Map"));
    }

    @Test
    @WithMockUser(authorities = "USER")
    void getMapsById_batch_returns200() throws Exception {
        when(mapService.getMapsById(any())).thenReturn(List.of(sampleMap()));

        mockMvc.perform(post("/api/maps/batch")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[1]"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(authorities = "USER")
    void getEnabledItemNumber_returns200() throws Exception {
        when(mapService.getEnabledItemsNumber()).thenReturn(5);

        mockMvc.perform(get("/api/maps/number"))
                .andExpect(status().isOk())
                .andExpect(content().string("5"));
    }

    @Test
    @WithMockUser(authorities = "USER")
    void getRecentEnabledMaps_returns200() throws Exception {
        when(mapService.getRecentEnabledMaps()).thenReturn(List.of(sampleMap()));

        mockMvc.perform(get("/api/maps/recent"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(authorities = "USER")
    void getMapStats_returns200() throws Exception {
        when(mapService.getMapsStats()).thenReturn(
                MapsStats.builder().totalMaps(10).borrowedMaps(2).availableMaps(8).build());

        mockMvc.perform(get("/api/maps/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalMaps").value(10));
    }

    @Test
    @WithMockUser(authorities = "USER")
    void getAllEnabledItems_returns200() throws Exception {
        when(mapService.getAllEnabledItems()).thenReturn(List.of(sampleMap()));

        mockMvc.perform(get("/api/maps/all"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(authorities = "USER")
    void setAvailableToBorrowed_availableMap_returns200() throws Exception {
        Map map = sampleMap();
        map.setAvailabilityStatus("AVAILABLE");
        when(mapService.getById(1)).thenReturn(map);

        mockMvc.perform(put("/api/maps/setAvailableToBorrowed")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LockItemRequest(1))))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(authorities = "USER")
    void setAvailableToBorrowed_alreadyBorrowed_returns400() throws Exception {
        Map map = sampleMap();
        map.setAvailabilityStatus("BORROWED");
        when(mapService.getById(1)).thenReturn(map);

        mockMvc.perform(put("/api/maps/setAvailableToBorrowed")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LockItemRequest(1))))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(authorities = "USER")
    void setBorrowedToAvailable_borrowedMap_returns200() throws Exception {
        Map map = sampleMap();
        map.setAvailabilityStatus("BORROWED");
        when(mapService.getById(1)).thenReturn(map);

        mockMvc.perform(put("/api/maps/setBorrowedToAvailable")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LockItemRequest(1))))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(authorities = "USER")
    void setBorrowedToAvailable_alreadyAvailable_returnsConflict() throws Exception {
        Map map = sampleMap();
        map.setAvailabilityStatus("AVAILABLE");
        when(mapService.getById(1)).thenReturn(map);

        mockMvc.perform(put("/api/maps/setBorrowedToAvailable")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LockItemRequest(1))))
                .andExpect(status().isConflict());
    }

    @Test
    void getMapById_unauthenticated_returns403() throws Exception {
        mockMvc.perform(get("/api/maps/1"))
                .andExpect(status().isForbidden());
    }
}
