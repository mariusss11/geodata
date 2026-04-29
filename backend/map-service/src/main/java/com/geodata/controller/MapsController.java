package com.geodata.controller;

import com.geodata.dto.MapsStats;
import com.geodata.model.AvailabilityStatus;
import com.geodata.model.Map;
import com.geodata.model.PagedResponse;
import com.geodata.service.MapService;
import com.geodata.utils.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
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

    @PostMapping("/batch")
    public ResponseEntity<List<Map>> getMapsById(@RequestBody List<Integer> mapIds) {
        return ResponseEntity.ok(mapService.getMapsById(mapIds));
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
    public PagedResponse<Map> getAllItemsPaginated(
            @RequestParam int pageNumber,
            @RequestParam int pageSize,
            @RequestParam(required = false) String searchQuery
    ) {
        return mapService.getAllEnabledItemsPaginatedBySearch(pageNumber, pageSize, searchQuery);
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

    @PutMapping("/setAvailableToBorrowed")
    public ResponseEntity<Void> setAvailableToBorrowed(@RequestBody LockItemRequest request) {
        log.info("Setting the availability of the map {} from available to borrowed", request);
        Map map = mapService.getById(request.getMapId());
        if (!map.getAvailabilityStatus().equalsIgnoreCase("AVAILABLE"))
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();

        // set it to pending to approve, because the librarian should approve the borrow
        map.setAvailabilityStatus(AvailabilityStatus.BORROWED.dbValue());
        mapService.saveItemVoid(map);
        return ResponseEntity.ok().build();
    }


    @PutMapping("/setBorrowedToAvailable")
    public ResponseEntity<Void> setBorrowedToAvailable(@RequestBody LockItemRequest request) {
        log.info("Setting the availability of the map {} from borrowed to available", request);
        Map map = mapService.getById(request.getMapId());
        if (map.getAvailabilityStatus().equalsIgnoreCase("AVAILABLE"))
            return ResponseEntity.status(HttpStatus.CONFLICT).build();

        // set it to pending to approve, because the librarian should approve the borrow
        map.setAvailabilityStatus(AvailabilityStatus.AVAILABLE.dbValue());
        mapService.saveItemVoid(map);
        return ResponseEntity.ok().build();
    }

}
