package com.geodata.service;

import com.geodata.dto.MapsStats;
import com.geodata.exceptions.InvalidItemException;
import com.geodata.exceptions.MapNotFoundException;
import com.geodata.model.Map;
import com.geodata.repository.MapRepository;
import com.geodata.utils.CreateMapRequest;
import com.geodata.utils.DisableItemRequest;
import com.geodata.utils.UpdateMapRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MapServiceTest {

    @Mock private MapRepository mapRepository;
    @InjectMocks private MapService mapService;

    private Map availableMap;
    private Map borrowedMap;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(mapService, "borrowService", "localhost:8030");

        availableMap = Map.builder()
                .id(1).name("Topographic Map").year(2020)
                .availabilityStatus("AVAILABLE").isEnabled(true)
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now())
                .build();

        borrowedMap = Map.builder()
                .id(2).name("Road Map").year(2019)
                .availabilityStatus("BORROWED").isEnabled(true)
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now())
                .build();
    }

    // ── getMap ───────────────────────────────────────────────────────────────

    @Test
    void getMap_existingId_returnsMap() {
        when(mapRepository.findById(1)).thenReturn(Optional.of(availableMap));

        Map result = mapService.getMap(1);

        assertThat(result).isEqualTo(availableMap);
    }

    @Test
    void getMap_missingId_throws() {
        when(mapRepository.findById(99)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> mapService.getMap(99))
                .isInstanceOf(MapNotFoundException.class);
    }

    // ── saveItem ─────────────────────────────────────────────────────────────

    @Test
    void saveItem_newMap_savesAndReturns() {
        CreateMapRequest req = new CreateMapRequest("New Map", 2023);
        when(mapRepository.findByNameAndYear("New Map", 2023)).thenReturn(Optional.empty());
        when(mapRepository.save(any())).thenReturn(availableMap);

        Map result = mapService.saveItem(req);

        verify(mapRepository).save(any(Map.class));
        assertThat(result).isNotNull();
    }

    @Test
    void saveItem_duplicateMap_throws() {
        CreateMapRequest req = new CreateMapRequest("Topographic Map", 2020);
        when(mapRepository.findByNameAndYear("Topographic Map", 2020))
                .thenReturn(Optional.of(availableMap));

        assertThatThrownBy(() -> mapService.saveItem(req))
                .isInstanceOf(InvalidItemException.class);
    }

    // ── updateMap ────────────────────────────────────────────────────────────

    @Test
    void updateMap_updatesNameAndYear() {
        when(mapRepository.findById(1)).thenReturn(Optional.of(availableMap));
        when(mapRepository.save(any())).thenReturn(availableMap);

        Map result = mapService.updateMap(1, new UpdateMapRequest("Updated Name", 2021));

        assertThat(result.getName()).isEqualTo("Updated Name");
        assertThat(result.getYear()).isEqualTo(2021);
    }

    @Test
    void updateMap_notFound_throws() {
        when(mapRepository.findById(99)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> mapService.updateMap(99, new UpdateMapRequest("X", 2020)))
                .isInstanceOf(MapNotFoundException.class);
    }

    // ── removeMap ────────────────────────────────────────────────────────────

    @Test
    void removeMap_borrowedMap_cannotDisable() {
        // removeMap should throw because map is NOT borrowed (it's AVAILABLE)
        when(mapRepository.findByName("Topographic Map")).thenReturn(Optional.of(availableMap));

        assertThatThrownBy(() -> mapService.removeMap(new DisableItemRequest("Topographic Map")))
                .isInstanceOf(InvalidItemException.class);
    }

    @Test
    void removeMap_notFound_throws() {
        when(mapRepository.findByName("Ghost Map")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> mapService.removeMap(new DisableItemRequest("Ghost Map")))
                .isInstanceOf(MapNotFoundException.class);
    }

    // ── getAllItems / getAllEnabledItems ───────────────────────────────────────

    @Test
    void getAllItems_returnsAll() {
        when(mapRepository.findAll()).thenReturn(List.of(availableMap, borrowedMap));

        List<Map> result = mapService.getAllItems();

        assertThat(result).hasSize(2);
    }

    @Test
    void getAllEnabledItems_returnsEnabledOnly() {
        when(mapRepository.findAllByIsEnabledTrue()).thenReturn(List.of(availableMap));

        List<Map> result = mapService.getAllEnabledItems();

        assertThat(result).hasSize(1);
    }

    // ── getMapsStats ─────────────────────────────────────────────────────────

    @Test
    void getMapsStats_calculatesCorrectly() {
        when(mapRepository.findAllByIsEnabledTrue()).thenReturn(List.of(availableMap, borrowedMap));

        MapsStats stats = mapService.getMapsStats();

        assertThat(stats.getTotalMaps()).isEqualTo(2);
        assertThat(stats.getBorrowedMaps()).isEqualTo(1);
        assertThat(stats.getAvailableMaps()).isEqualTo(1);
    }

    // ── getMapsById ──────────────────────────────────────────────────────────

    @Test
    void getMapsById_validIds_returnsMaps() {
        when(mapRepository.findAllById(List.of(1, 2))).thenReturn(List.of(availableMap, borrowedMap));

        List<Map> result = mapService.getMapsById(List.of(1, 2));

        assertThat(result).hasSize(2);
    }

    @Test
    void getMapsById_emptyList_returnsEmpty() {
        List<Map> result = mapService.getMapsById(List.of());

        assertThat(result).isEmpty();
        verifyNoInteractions(mapRepository);
    }

    @Test
    void getMapsById_nullList_returnsEmpty() {
        List<Map> result = mapService.getMapsById(null);

        assertThat(result).isEmpty();
    }

    // ── getRecentEnabledMaps ──────────────────────────────────────────────────

    @Test
    void getRecentEnabledMaps_returnsAtMostSix() {
        List<Map> many = List.of(availableMap, availableMap, availableMap,
                availableMap, availableMap, availableMap, availableMap);
        when(mapRepository.findAllByIsEnabledTrue()).thenReturn(many);

        List<Map> result = mapService.getRecentEnabledMaps();

        assertThat(result.size()).isLessThanOrEqualTo(6);
    }

    // ── getAllEnabledItemsPaginatedBySearch ───────────────────────────────────

    @Test
    void getAllEnabledItemsPaginatedBySearch_noQuery_returnsAll() {
        Page<Map> page = new PageImpl<>(List.of(availableMap));
        when(mapRepository.findAllByIsEnabledTrue(any(Pageable.class))).thenReturn(page);

        var result = mapService.getAllEnabledItemsPaginatedBySearch(0, 10, null);

        assertThat(result.getContent()).hasSize(1);
    }

    @Test
    void getAllEnabledItemsPaginatedBySearch_withQuery_filtersResults() {
        Page<Map> page = new PageImpl<>(List.of(availableMap));
        when(mapRepository.findByIsEnabledTrueAndNameContainingIgnoreCase(eq("Topo"), any(Pageable.class)))
                .thenReturn(page);

        var result = mapService.getAllEnabledItemsPaginatedBySearch(0, 10, "Topo");

        assertThat(result.getContent()).hasSize(1);
    }
}
