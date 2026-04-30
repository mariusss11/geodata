package com.geodata.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.geodata.model.Map;
import com.geodata.security.config.MapServiceSecurityConfig;
import com.geodata.security.jwt.JwtUtils;
import com.geodata.service.MapService;
import com.geodata.utils.CreateMapRequest;
import com.geodata.utils.DisableItemRequest;
import com.geodata.utils.UpdateMapRequest;
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

@WebMvcTest(MapsManagementController.class)
@Import(MapServiceSecurityConfig.class)
class MapsManagementControllerTest {

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
    @WithMockUser(authorities = {"ADMIN", "USER"})
    void createMap_returns200() throws Exception {
        when(mapService.saveItem(any())).thenReturn(sampleMap());

        mockMvc.perform(post("/api/maps/manager/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateMapRequest("Topo Map", 2020))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Topographic Map"));
    }

    @Test
    @WithMockUser(authorities = {"ADMIN", "USER"})
    void updateMap_returns200() throws Exception {
        Map updated = sampleMap();
        updated.setName("Updated Map");
        when(mapService.updateMap(eq(1), any())).thenReturn(updated);

        mockMvc.perform(put("/api/maps/manager/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdateMapRequest("Updated Map", 2021))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated Map"));
    }

    @Test
    @WithMockUser(authorities = {"ADMIN", "USER"})
    void disableMap_returns200() throws Exception {
        when(mapService.removeMap(any())).thenReturn("The item was disabled successfully");

        mockMvc.perform(put("/api/maps/manager/disable")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new DisableItemRequest("Topo Map"))))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(authorities = {"ADMIN", "MANAGER"})
    void getAllItems_returns200() throws Exception {
        when(mapService.getAllItems()).thenReturn(List.of(sampleMap()));

        mockMvc.perform(get("/api/maps/manager/all"))
                .andExpect(status().isOk());
    }

    @Test
    void createMap_unauthenticated_returns403() throws Exception {
        mockMvc.perform(post("/api/maps/manager/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateMapRequest("Topo Map", 2020))))
                .andExpect(status().isForbidden());
    }
}
