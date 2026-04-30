package com.geodata.service;

import com.geodata.dto.MapsStats;
import com.geodata.exceptions.*;
import com.geodata.model.*;
import com.geodata.repository.MapRepository;
import com.geodata.utils.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;


@Slf4j
@Service
public class MapService {

    private final MapRepository mapRepository;
    @Value("${services.borrowService}")
    private String borrowService;

    @Autowired
    public MapService(MapRepository mapRepository) {
        this.mapRepository = mapRepository;
    }


    public Map getMap(int itemId) {
        log.info("Returning the item with the id: {}", itemId);
        return getById(itemId);
    }

    /**
     * Method just for the admin and librarian
     * @return all the items
     */
    public List<Map> getAllItems() {
        return mapRepository.findAll();
    }

    /**
     * Method for the user
     * @return all the enabled items
     */
    public List<Map> getAllEnabledItems() {
        return mapRepository.findAllByIsEnabledTrue();
    }

    /**
     * Method to update the item
     * @param item the item that got updated
     */
    public void saveItemVoid(Map item) {
        mapRepository.save(item);
    }

    public Map saveItem(CreateMapRequest request) {
        Map map = getMap(request);
        log.info("Item to save {}", map);

        if (mapRepository.findByNameAndYear(request.getName(), map.getYear()).isPresent())
            throw new InvalidItemException("The map is already in the library");

        // save the map
        mapRepository.save(map);

        return map;
    }

    private Map getMap(CreateMapRequest request) {
        return Map.builder()
                .name(request.getName())
                .year(request.getYearPublished())
                .availabilityStatus("AVAILABLE")
                .isEnabled(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }


//    TODO: think about the thing when someone wants to delete a map that has the same name as other one
    public Map updateMap(int id, UpdateMapRequest request) {
        Map map = getById(id);
        map.setName(request.getName());
        map.setYear(request.getYearPublished());
        map.setUpdatedAt(LocalDateTime.now());
        mapRepository.save(map);
        return map;
    }

    public String removeMap(DisableItemRequest request) {
        Map mapToDisable = getMapByName(request.getName());
        // check if the item is borrowed
        if (!mapToDisable.getAvailabilityStatus().equalsIgnoreCase("BORROWED"))
            throw new InvalidItemException("Cannot remove a borrowed item: " + mapToDisable);

        mapToDisable.setEnabled(false);
        mapRepository.save(mapToDisable);
        return "The item was disabled successfully";
    }

    public Map getById(int itemId) {
        return mapRepository.findById(itemId).orElseThrow(() -> new MapNotFoundException("Item was not found"));
    }

    Map getMapByName(String name) {
        Map searchItem = mapRepository.findByName(name)
                .orElseThrow(() -> new MapNotFoundException("Not found map with name: " + name));
        log.info("Searched item: {}", searchItem);
        return searchItem;
    }

    public Integer getEnabledItemsNumber() {
        return getAllEnabledItems().size();
    }

    public List<Map> getRecentEnabledMaps() {
        return getAllEnabledItems().stream()
                .filter(item -> isRecent(item.getCreatedAt()))
                                .limit(6).toList();
    }

    /**
     * Checks whether an item is considered recently added.
     * <p>
     * An item is considered recent if it was added within the last 30 days
     * from the current date.
     *
     * @param createdDate the date and time when the item was created
     * @return {@code true} if the item was created within the last 30 days, {@code false} otherwise
     */
    private boolean isRecent(LocalDateTime createdDate) {
        LocalDate now = LocalDate.now();
        int dayDifference = now.getDayOfYear() - createdDate.getDayOfYear();
        return dayDifference <= 30 && createdDate.getYear() == now.getYear();
    }

    public PagedResponse<Map> getAllEnabledItemsPaginated(int pageNumber, int pageSize, String searchQuery) {
        Pageable pageable = PageRequest.of(pageNumber, pageSize);
        Page<Map> itemsPage = mapRepository.findAll(pageable);
        return new PagedResponse<>(itemsPage);
    }

    public PagedResponse<Map> getAllEnabledItemsPaginatedBySearch(int pageNumber, int pageSize, String query) {
        log.info("In the getAllEnabledItemsPaginatedBySearch method");

        Page<Map> page;
        Pageable pageable = PageRequest.of(pageNumber, pageSize);

        if (query == null || query.trim().isEmpty()) {
            page = mapRepository.findAllByIsEnabledTrue(pageable);
        } else {
            log.info("the query is: {}", query);
            page = mapRepository
                    .findByIsEnabledTrueAndNameContainingIgnoreCase(query, pageable);
        }
        return new PagedResponse<>(page);
    }

    public MapsStats getMapsStats() {
        List<Map> totalMaps = mapRepository.findAllByIsEnabledTrue();
        List<Map> borrowedMaps = mapRepository.findAllByIsEnabledTrue().stream()
                .filter(map -> !map.getAvailabilityStatus().equalsIgnoreCase("AVAILABLE"))
                .toList();


        return MapsStats.builder()
                .totalMaps(totalMaps.size())
                .borrowedMaps(borrowedMaps.size())
                .availableMaps(totalMaps.size() - borrowedMaps.size())
                .build();
    }

    public List<Map> getMapsById(List<Integer> mapIds) {
        if (mapIds == null || mapIds.isEmpty()) {
            return List.of();
        }

        return mapRepository.findAllById(mapIds)
                .stream()
                .toList();
    }
}
