package com.geodata.controller;

import com.geodata.dto.MapsStats;
import com.geodata.model.Map;
import com.geodata.model.PagedResponse;
import com.geodata.service.MapService;
import com.geodata.utils.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/maps")
public class MapsController {

    private final MapService mapService;

    @GetMapping()
    public ResponseEntity<Map> getItemWithRequestBody(@RequestBody GetItemRequest request) {
        return ResponseEntity.ok(mapService.getMap(request.getId()));
    }

    @GetMapping("/all/pagination")
    public PagedResponse<Map> getAllItemsPaginated(
            @RequestParam int pageNumber,
            @RequestParam int pageSize
    ) {
        return mapService.getAllEnabledItemsPaginated(pageNumber, pageSize);
    }

    @GetMapping("/number")
    public ResponseEntity<Integer> getEnabledItemNumber() {
        return ResponseEntity.ok(mapService.getEnabledItemsNumber());
    }

    @GetMapping("/recent")
    public ResponseEntity<List<Map>> getRecentEnabledMaps() {
        return ResponseEntity.ok(mapService.getRecentEnabledMaps());
    }

    @GetMapping("/stats")
    public ResponseEntity<MapsStats> getMapStats() {
        return ResponseEntity.ok(mapService.getMapsStats());
    }

    @GetMapping("/{mapId}")
    public ResponseEntity<Map> getItemById(@PathVariable int mapId) {
        return ResponseEntity.ok(mapService.getMap(mapId));
    }

//    @GetMapping("/search")
//    public ResponseEntity<List<Map>> getItemsByQuery(@RequestParam(name = "query") String searchQuery) {
//        return ResponseEntity.ok(mapService.getItemsByQuery(searchQuery));
//    }

    @GetMapping("/search/pagination")
    public PagedResponse<Map> getAllItemsPaginated(Pageable pageable, @RequestParam String query) {
        return mapService.getAllEnabledItemsPaginatedBySearch(pageable, query);
    }

//    @GetMapping("/params")
//    public ResponseEntity<Map> getItemWithRequestParams(
//            @RequestParam String name
//    ) {
//        return ResponseEntity.ok(mapService.getMap(null);
//    }

//    @GetMapping("/list/id")
//    public ResponseEntity<Set<Integer>> getItemIdsByQuery(@RequestParam("query") String query) {
//        return ResponseEntity.ok(mapService.getItemIdsByQuery(query));
//    }
//
//    @GetMapping("/filteredBy")
//    public ResponseEntity<List<Map>> getItemsFilteredByCategory(@RequestParam String category) {
//        return ResponseEntity.ok(mapService.getAllItemsByCategory(category));
//    }

    @GetMapping("/all")
    public ResponseEntity<List<Map>> getAllEnabledItems() {
        log.info("Returning all the enabled maps: {}", SecurityContextHolder.getContext().getAuthentication().getName());
        return ResponseEntity.ok(mapService.getAllEnabledItems());
    }

}
