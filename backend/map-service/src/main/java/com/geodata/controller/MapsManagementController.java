package com.geodata.controller;

import com.geodata.model.Map;
import com.geodata.service.MapService;
import com.geodata.utils.CreateMapRequest;
import com.geodata.utils.DisableItemRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RequestMapping("/api/maps/manager")
@RestController
public class MapsManagementController {


    private final MapService mapService;

    @Autowired
    public MapsManagementController(MapService mapService) {
        this.mapService = mapService;
    }

    @PostMapping("/create")
    public ResponseEntity<Map> createItem(@RequestBody CreateMapRequest request) {
        return ResponseEntity.ok(mapService.saveItem(request));
    }

    @PutMapping("/disable")
    public ResponseEntity<String> disableItem(@RequestBody DisableItemRequest request) {
        return ResponseEntity.ok(mapService.removeMap(request));
    }

    @GetMapping("/all")
    public ResponseEntity<List<Map>> getAllItems() {
        log.info("Returning all the items: {}", SecurityContextHolder.getContext().getAuthentication().getName());
        return ResponseEntity.ok(mapService.getAllItems());
    }

}
