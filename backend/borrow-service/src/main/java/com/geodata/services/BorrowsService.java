package com.geodata.services;

import com.geodata.exceptions.*;
import com.geodata.model.*;
import com.geodata.model.PagedResponse;
import com.geodata.repository.BorrowsRepository;
import com.geodata.security.jwt.JwtUtils;
import com.geodata.utils.*;
import com.geodata.utils.requests.BorrowMapRequest;
import com.geodata.utils.requests.LockItemRequest;
import com.geodata.utils.requests.ReturnMapRequest;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

// https://www.baeldung.com/spring-rest-template-error-handling
// https://howtodoinjava.com/spring-boot2/resttemplate/spring-restful-client-resttemplate-example/
@Slf4j
@Service
public class BorrowsService {

    private static final String HTTP = "http://";
    private final BorrowsRepository borrowsRepository;
    private final RestTemplate restTemplate;
    private final JwtUtils jwtUtils;
    private final HttpServletRequest httpRequest;
    @Value("${services.identityService}")
    private String userService;
    @Value("${services.mapService}")
    private String mapService;


    @Autowired
    public BorrowsService(BorrowsRepository borrowsRepository, RestTemplate restTemplate, JwtUtils jwtUtils, HttpServletRequest httpRequest) {
        this.borrowsRepository = borrowsRepository;
        this.restTemplate = restTemplate;
        this.jwtUtils = jwtUtils;
        this.httpRequest = httpRequest;
    }

    public ResponseEntity<String> borrowMap(BorrowMapRequest request) {
        log.info("Borrowing map: {}", request);

        if (request.getReturnDate().isBefore(LocalDate.now()))
            throw new InvalidReturnDate("Invalid return date: " + request.getReturnDate());

        String token = getAuthToken();
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        Map mapToBorrow = null;

        try {
            mapToBorrow = getMapInfo(request.getMapId(), entity);

            if (!mapToBorrow.getAvailabilityStatus().equalsIgnoreCase("AVAILABLE") || !mapToBorrow.isEnabled())
                throw new MapNotAvailableException("Map not available");

            HttpHeaders postHeaders = new HttpHeaders();
            postHeaders.setContentType(MediaType.APPLICATION_JSON);
            postHeaders.setBearerAuth(token);

            LockItemRequest lockRequest = new LockItemRequest(mapToBorrow.getId());
            HttpEntity<LockItemRequest> lockEntity = new HttpEntity<>(lockRequest, postHeaders);
            changeMapStatusFromAvailableToBorrowed(lockEntity);

            borrowsRepository.save(
                    Borrows.builder()
                            .borrowerName(request.getBorrowerName())
                            .mapId(mapToBorrow.getId())
                            .borrowDate(LocalDateTime.now())
                            .returnDate(request.getReturnDate())
                            .status(BorrowsStatus.BORROWED.dbValue())
                            .build()
            );

            log.info("Borrow completed successfully for borrower '{}'", request.getBorrowerName());

        } catch (ItemServiceException | MapNotAvailableException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (Exception e) {
            log.error("Borrow saga failed: {}", e.getMessage());

            try {
                if (mapToBorrow != null) {
                    HttpHeaders unlockHeaders = new HttpHeaders();
                    unlockHeaders.setContentType(MediaType.APPLICATION_JSON);
                    unlockHeaders.setBearerAuth(token);
                    LockItemRequest unlockRequest = new LockItemRequest(mapToBorrow.getId());
                    HttpEntity<LockItemRequest> unlockEntity = new HttpEntity<>(unlockRequest, unlockHeaders);
                    restTemplate.exchange(HTTP + mapService + "/api/maps/setBorrowedToAvailable",
                            HttpMethod.PUT, unlockEntity, Void.class);
                }
            } catch (Exception unlockEx) {
                log.error("Failed to rollback map lock: {}", unlockEx.getMessage());
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error during rollback");
            }
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
        return ResponseEntity.ok("Borrow started successfully");
    }

    private Map getMapInfo(int mapId, HttpEntity<Void> entity) {
        log.info("Requesting the map info from the map-service");
        Map map = null;

        // requesting the item to borrow
        ResponseEntity<Map> mapServiceResponse = restTemplate
                .exchange(
                        HTTP + mapService + "/api/maps/{mapId}",
                        HttpMethod.GET,
                        entity,
                        Map.class,
                        mapId
                );

        if (mapServiceResponse.getBody() == null)
            throw new ItemServiceException("Item service response is empty");

        map = mapServiceResponse.getBody();
        return map;
    }


    private void changeMapStatusFromAvailableToBorrowed(HttpEntity<LockItemRequest> lockEntity) {
        ResponseEntity<Void> updateResponse = restTemplate.exchange(
                HTTP + mapService + "/api/maps/setAvailableToBorrowed",
                HttpMethod.PUT,
                lockEntity,
                Void.class
        );

        if (updateResponse.getStatusCode().isError())
            throw new ItemServiceException("Error occurred when changing the status on the item");
    }

    private void changeMapStatusFromBorrowedToAvailable(HttpEntity<LockItemRequest> entity) {
        ResponseEntity<Void> updateResponse = restTemplate.exchange(
                HTTP + mapService + "/api/maps/setBorrowedToAvailable",
                HttpMethod.PUT,
                entity,
                Void.class
        );

        if (updateResponse.getStatusCode().isError())
            throw new ItemServiceException("Error occurred when changing the status on the item");
    }


    public ResponseEntity<String> returnMap(ReturnMapRequest request) {
        log.info("Returning borrow record id: {}", request.getBorrowId());

        String token = getAuthToken();

        Borrows borrowRecord = getBorrowById(request.getBorrowId());

        if (!borrowRecord.getStatus().equalsIgnoreCase(BorrowsStatus.BORROWED.dbValue()))
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("This borrow record is not in BORROWED status");

        try {
            HttpHeaders putHeaders = new HttpHeaders();
            putHeaders.setContentType(MediaType.APPLICATION_JSON);
            putHeaders.setBearerAuth(token);

            LockItemRequest lockRequest = new LockItemRequest(borrowRecord.getMapId());
            HttpEntity<LockItemRequest> updateEntity = new HttpEntity<>(lockRequest, putHeaders);
            changeMapStatusFromBorrowedToAvailable(updateEntity);

            borrowRecord.setStatus(BorrowsStatus.RETURNED.dbValue());
            borrowRecord.setActualReturnDate(LocalDateTime.now());
            borrowsRepository.save(borrowRecord);

            log.info("Return completed for borrow id: {}", request.getBorrowId());

        } catch (ItemServiceException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (Exception e) {
            log.error("Exception during returnMap, compensating: {}", e.getMessage());
            try {
                HttpHeaders putHeaders = new HttpHeaders();
                putHeaders.setContentType(MediaType.APPLICATION_JSON);
                putHeaders.setBearerAuth(token);
                LockItemRequest lockRequest = new LockItemRequest(borrowRecord.getMapId());
                HttpEntity<LockItemRequest> updateEntity = new HttpEntity<>(lockRequest, putHeaders);
                changeMapStatusFromAvailableToBorrowed(updateEntity);
            } catch (Exception lockEx) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error during return rollback");
            }
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Error during return. Please try again.");
        }
        return ResponseEntity.ok("Map successfully returned");
    }

    public long removeItemByIdFromBorrowsList(int itemId) {
        log.info("Removing the item from the borrowed list who's id is: {}", itemId);
        return borrowsRepository.deleteByMapId(itemId);
    }

    public List<Integer> getClientBorrowedItems(int clientId) {
        log.info("Trying to return client borrowed items");
        List<Borrows> borrowsList = borrowsRepository.findAllByUserIdAndStatus(clientId, BorrowsStatus.RETURNED.dbValue());
        List<Integer> itemIdList = new ArrayList<>();
        borrowsList.forEach(borrows -> itemIdList.add(borrows.getMapId()));
        log.info("Returning client borrowed items");
        return itemIdList;
    }

    public List<Object[]> findMostBorrowedItemsByClient(int clientId) {
        return borrowsRepository.findMostBorrowedItemsByUser(clientId);
    }

    /**
     * Retrieves the list of item IDs currently borrowed by a specific client.
     * <p>
     * This method queries the database for all borrow records associated with the given client ID
     * where the borrow status is {@code BORROWED} (i.e., not yet returned). It then extracts and
     * returns the item IDs from those borrowed records.
     * </p>
     *
     * @return a list of item IDs that the specified client has currently borrowed and not yet returned
     */
    public PagedResponse<BorrowedMapDto> getUserCurrentBorrows(int pageNumber, int pageSize, String query) {
        Pageable pageable = PageRequest.of(pageNumber, pageSize);
        Page<Borrows> list = null;

        if (query == null || query.trim().isEmpty()) {
            list = borrowsRepository.findAllByStatus(BorrowStatus.BORROWED.dbValue(), pageable);
        } else {
            log.info("the query is: {}", query);
//            page = borrowsRepository.findByIsEnabledTrueAndNameContainingIgnoreCase(query, pageable);
        }
        if (list == null)
            return new PagedResponse<>();

        List<Integer> mapIds = list.getContent()
                .stream()
                .map(Borrows::getMapId)
                .toList();


        log.info("Making the request to get the maps with the ids: {}", mapIds);


        String authHeader = httpRequest.getHeader("Authorization");
        String token = jwtUtils.getToken(authHeader);

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<List<Integer>> entity = new HttpEntity<>(mapIds, headers);

        List<Map> maps = null;

        try {

        ResponseEntity<List<Map>> response = restTemplate.exchange(
                HTTP + mapService + "/api/maps/batch",
                HttpMethod.POST,
                entity,
                new ParameterizedTypeReference<>() {}
        );

        maps = response.getBody();
        } catch (RuntimeException e) {
            throw new MapServiceException("An error occurred when fetching the maps");
        }

        List<Map> finalMaps = maps;
        assert finalMaps != null;

        List<BorrowedMapDto> borrowedMapDTOS = list.getContent().stream()
                .map(borrow -> {
                    Map map = finalMaps.stream()
                            .filter(m -> m.getId() == borrow.getMapId())
                            .findFirst()
                            .orElse(null);
                    if (map == null) return null;
                    return BorrowedMapDto.builder()
                            .borrowId(borrow.getId())
                            .mapId(map.getId())
                            .borrowerName(borrow.getBorrowerName())
                            .name(map.getName())
                            .year(map.getYear())
                            .isEnabled(map.isEnabled())
                            .availabilityStatus(map.getAvailabilityStatus())
                            .createdAt(map.getCreatedAt())
                            .updatedAt(map.getUpdatedAt())
                            .build();
                })
                .filter(Objects::nonNull)
                .toList();


        return new PagedResponse<>(
                borrowedMapDTOS,
                list.getNumber(),
                list.getSize(),
                list.getTotalElements(),
                list.getTotalPages(),
                list.isLast(),
                list.isFirst(),
                list.hasNext(),
                list.hasPrevious()
        );
        }

//    public List<BorrowsDTO> getClientMapsCurrentlyBorrowed(int clientId, String query) {
//        log.info("Getting client items that are currently being borrowed with query: {}", query);
//        List<LibraryItem> itemsByQuery = getItemsByQuery(query);
//
//        // Create a lookup map: itemId -> LibraryItem
//        Map<Integer, LibraryItem> itemMap = itemsByQuery.stream()
//                .collect(Collectors.toMap(LibraryItem::getId, item -> item));
//
//        // The item IDs
//        List<Integer> itemIds = new ArrayList<>(itemMap.keySet());
//
//        List<Borrows> borrowsList = borrowsRepository.findBorrowsCurrentlyBorrowedByClientFiltered(clientId, itemIds);
//
//        List<BorrowsDTO> result = new ArrayList<>();
//        for (Borrows borrow : borrowsList) {
//            LibraryItem item = itemMap.get(borrow.getMapId());
//            BorrowsDTO dto = BorrowMapper.toDto(borrow, item);
//            result.add(dto);
//        }
//
//        log.info("Returning client's items that are currently being borrowed");
//        // return the list of the item ids
//        return result;
//    }

    /**
     * We send the list of items that were seen in the query in the frontend,
     * and now the backend just sees which of the items are borrowed by the client
     */
//    public List<Integer> getClientAllCurrentBorrowsByIdAndItemsIdList(int clientId, List<Integer> itemIdList) {
//        // get the list of the current borrow that is not returned
//        List<Borrows> borrowItems = borrowsRepository.findBorrowMapsByUserIdAndMapIdAndStatus(clientId, BorrowsStatus.BORROWED.dbValue());
//
//        // return the list of the item ids
//        return borrowItems.stream().map(Borrows::getMapId).toList();
//    }

    public Boolean hadBorrowedItem(int clientId, int itemId) {
        return !borrowsRepository.findByUserIdAndMapIdAndStatus(clientId, itemId, BorrowsStatus.RETURNED.dbValue()).isEmpty();
    }


    private String getAuthToken() {
        String authHeader = httpRequest.getHeader("Authorization");
        return jwtUtils.getToken(authHeader);
    }

    private void setBorrowToPending(HttpEntity<?> entity) {
        try {
            ResponseEntity<Void> updateResponse = restTemplate.exchange(
                    HTTP + mapService + userService + "/api/items/setBorrowedToPending",
                    HttpMethod.PUT,
                    entity,
                    Void.class
            );
            log.info("Status changed successfully to the item: {}", updateResponse.getStatusCode());
        } catch (Exception e) {
            throw new ChangeItemStatusException("An error occurred when trying to change the status of an item");
        }
    }

//    public List<BorrowActionsDTO> getClientReturnedItemsFiltered(int clientId, String query) {
//        log.info("Getting client returned items filtered by query: {}", query);
//        // Items filtered by the query
//        List<LibraryItem> itemsByQuery = getItemsByQuery(query);
//
//        // Create a lookup map: itemId -> LibraryItem
//        Map<Integer, LibraryItem> itemMap = itemsByQuery.stream()
//                .collect(Collectors.toMap(LibraryItem::getId, item -> item));
//
//        // The item IDs
//        List<Integer> itemIds = new ArrayList<>(itemMap.keySet());
//
//        // Filtered borrow actions
//        List<BorrowActions> borrowActionsFiltered = borrowActionsService.getClientReturnedItemsFiltered(clientId, itemIds);
//        log.info("Got the borrow actions filtered: {}", borrowActionsFiltered);
//
//        log.info("Starting the mapping of the borrowAction list with the dto list");
//
//        // Result list
//        List<BorrowActionsDTO> result = new ArrayList<>();
//
//        for (BorrowActions borrowAction : borrowActionsFiltered) {
//            // Get the item by id
//            LibraryItem item = itemMap.get(borrowAction.getItemId());
//            log.info("Item that will be mapped into the borrow action: {}", item);
//            BorrowActionsDTO dto = BorrowActionsMapper.toDto(borrowAction, item);
//            result.add(dto);
//        }
//        log.info("Returning client's returned items filtered: {}", result);
//        return result;
//    }

    private List<LibraryItem> getItemsByQuery(String query) {
        log.info("Getting items by query: {}", query);
        String token = getAuthToken();
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        ResponseEntity<List<LibraryItem>> itemServiceResponse = restTemplate.exchange(
                HTTP + mapService + "/api/items/search?query={query}",
                HttpMethod.GET,
                entity,
                new ParameterizedTypeReference<>() {},
                query
        );

        if (itemServiceResponse.getBody() == null)
            throw new ItemServiceException("Item service exception is empty");

        log.info("Items fetched successfully: {}", itemServiceResponse.getBody());
        return itemServiceResponse.getBody();
    }

    @Transactional
    public TransferResponse transferBorrowedItem(TransferMapRequest request) {
        Borrows borrow = getBorrowById(request.getBorrowId());

        if (!borrow.getStatus().equalsIgnoreCase(BorrowStatus.BORROWED.dbValue()))
            throw new TransferMapException("Cannot transfer: map is not currently borrowed");

        String token = getAuthToken();
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        Map mapTransferred = getMapInfo(borrow.getMapId(), entity);

        if (!mapTransferred.canBeTransferred())
            throw new InvalidMapException("Map cannot be transferred!");

        borrow.setStatus(BorrowStatus.TRANSFERRED.dbValue());
        borrow.setActualReturnDate(LocalDateTime.now());
        borrowsRepository.save(borrow);

        Borrows newBorrow = Borrows.builder()
                .borrowDate(LocalDateTime.now())
                .borrowerName(request.getNewBorrowerName())
                .mapId(borrow.getMapId())
                .returnDate(request.getNewExpectedReturnDate())
                .status(BorrowStatus.BORROWED.dbValue())
                .build();
        borrowsRepository.save(newBorrow);

        log.info("Map {} transferred from '{}' to '{}'", borrow.getMapId(), borrow.getBorrowerName(), request.getNewBorrowerName());

        return TransferResponse.builder()
                .newBorrow(newBorrow)
                .newBorrowerName(request.getNewBorrowerName())
                .mapTransferred(mapTransferred)
                .build();
    }

    private Borrows getBorrowById(int borrowId) {
        return borrowsRepository.findById(borrowId)
                .orElseThrow(() -> new BorrowNotFoundException("Borrow not found"));
    }

}
