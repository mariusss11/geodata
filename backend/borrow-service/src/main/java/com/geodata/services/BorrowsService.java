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
import org.springframework.security.access.AccessDeniedException;
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
    @Value("${services.clientService}")
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
        log.info("Borrowing item: {}", request);

        if (request.getReturnDate().isBefore(LocalDate.now()))
            throw new InvalidReturnDate("Invalid return date: " + request.getReturnDate());

        String token = getAuthToken();
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        HttpEntity<Void> entity = new HttpEntity<>(headers);


        // defining the object that will be used
        Map mapToBorrow= null;
        User userThatBorrows;
        Borrows borrowRecord = null;

        try {

            log.info("Requesting the user info");

            // requesting the client
            ResponseEntity<User> userResponse = restTemplate
                    .exchange(
                            HTTP + userService + "/api/home",
                            HttpMethod.GET,
                            entity,
                            User.class
                    );

            if (userResponse.getStatusCode().isSameCodeAs(HttpStatus.NOT_FOUND))
                throw new UserNotFoundException("User not found");


            if (userResponse.getBody() == null)
                throw new UserServiceException("User service response body is empty");

            log.info("The user response who borrows: {}", userResponse);
            userThatBorrows = userResponse.getBody();
            log.info("The user that borrows: {}", userThatBorrows);



            log.info("Requesting the map info");

            // requesting the item to borrow
            ResponseEntity<Map> itemResponse = restTemplate
                    .exchange(
                            HTTP + mapService + "/api/maps/{mapId}",
                            HttpMethod.GET,
                            entity,
                            Map.class,
                            request.getMapId()
                    );

            if (itemResponse.getBody() == null)
                throw new ItemServiceException("Item service response is empty");

            mapToBorrow = itemResponse.getBody();
            if (!mapToBorrow.getAvailabilityStatus().equalsIgnoreCase("AVAILABLE") || !mapToBorrow.isEnabled())
                throw new MapNotAvailableException("Map not available");


            // START MAKING CHANGES
            // lock the item (availability set to PENDING_APPROVAL)
            HttpHeaders postHeaders = new HttpHeaders();
            postHeaders.setContentType(MediaType.APPLICATION_JSON);
            postHeaders.setBearerAuth(token);

            LockItemRequest lockRequest = new LockItemRequest(mapToBorrow.getId());
            HttpEntity<LockItemRequest> lockEntity = new HttpEntity<>(lockRequest, postHeaders);

            ResponseEntity<Void> updateResponse = restTemplate.exchange(
                    HTTP + mapService + "/api/maps/setAvailableToBorrowed",
                    HttpMethod.PUT,
                    lockEntity,
                    Void.class
            );

            if (updateResponse.getStatusCode().isError())
                throw new ItemServiceException("Error occurred when changing the status on the item");

            // STEP 4: Save borrow record
            borrowRecord = borrowsRepository.save(
                    Borrows.builder()
                            .userId(userThatBorrows.getUserId())
                            .mapId(mapToBorrow.getId())
                            .borrowDate(LocalDateTime.now())
                            .returnDate(request.getReturnDate())
                            .status(BorrowsStatus.BORROWED.dbValue())
                            .build()
            );

            log.info("Borrow saga completed successfully. Borrow record create: {}", borrowRecord);

            // Catching
            // checking if the borrow failed because the item is already borrowed,
            // or the client is not found
        } catch (ItemServiceException | UserServiceException | MapNotAvailableException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (Exception e) {

            log.info("Saga failed: {}", e.getMessage());

            // COMPENSATIONS STEP 1: Unlock item if locked (set the availability to available
            try {
                HttpHeaders unlockHeaders = new HttpHeaders();
                unlockHeaders.setContentType(MediaType.APPLICATION_JSON);
                unlockHeaders.setBearerAuth(token);

                assert mapToBorrow != null;
                LockItemRequest unlockRequest = new LockItemRequest(mapToBorrow.getId());
                HttpEntity<LockItemRequest> unlockEntity = new HttpEntity<>(unlockRequest, unlockHeaders);

                restTemplate.exchange(
                        HTTP + mapService + "/api/maps/setBorrowedToAvailable",
                        HttpMethod.PUT,
                        unlockEntity,
                        Void.class
                );

            } catch (Exception unlockEx) {
                log.error("Failed to rollback item lock: {}", unlockEx.getMessage());
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error occurred when changing the status for compensation");
            }
            log.info("Rolled back every action because an error occurred when making a borrow request");
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
        log.info("Returning map with id: {}", request.getMapId());

        String authHeader = httpRequest.getHeader("Authorization");
        String token = jwtUtils.getToken(authHeader);

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        Map mapToReturn = null;
        User userThatReturns;
        Borrows borrowRecord;

        try {

            // request the map
            mapToReturn = getMapInfo(request.getMapId(), entity);

            if (mapToReturn.getAvailabilityStatus().equalsIgnoreCase("AVAILABLE") || !mapToReturn.isEnabled())
                throw new MapNotAvailableException("Map is not borrowed");

            userThatReturns = getUserInfo(entity);

            // check if this client had borrowed the item
            borrowRecord = borrowsRepository.findByUserIdAndMapIdAndStatus(
                            userThatReturns.getUserId(),
                            mapToReturn.getId(),
                            BorrowsStatus.BORROWED.dbValue()).stream().findFirst()
                    .orElseThrow(() -> new RuntimeException("You haven't borrowed this map"));


            // start making some changes
            // unlock the item (set the status to pending)
            HttpHeaders putHeaders = new HttpHeaders();
            putHeaders.setContentType(MediaType.APPLICATION_JSON);
            putHeaders.setBearerAuth(token);

            LockItemRequest lockRequest = new LockItemRequest(mapToReturn.getId());
            HttpEntity<LockItemRequest> updateEntity = new HttpEntity<>(lockRequest, putHeaders);

            try {
                changeMapStatusFromBorrowedToAvailable(updateEntity);
            } catch (Exception e) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body("Error occurred when changing the status of hte item during request to return an item");
            }

            // update the borrow record
            borrowRecord.setStatus(BorrowsStatus.RETURNED.dbValue());
            borrowsRepository.save(borrowRecord);

            log.info("Return saga completed successfully");

        } catch (ItemServiceException | UserServiceException | MapNotAvailableException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (Exception e) {
            log.error("Exception during returnItem, starting compensation: {}", e.getMessage());

            // Compensation steps
            // Trying to change the status back to how it was
            try {
                HttpHeaders putHeaders = new HttpHeaders();
                putHeaders.setContentType(MediaType.APPLICATION_JSON);
                putHeaders.setBearerAuth(token);

                assert mapToReturn != null;
                LockItemRequest lockRequest = new LockItemRequest(mapToReturn.getId());
                HttpEntity<LockItemRequest> updateEntity = new HttpEntity<>(lockRequest, putHeaders);
                changeMapStatusFromAvailableToBorrowed(updateEntity);
                log.info("Compensation: locked the item again");
            } catch (Exception lockEx) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error occurred when changing the status for compensation when returning item");
            }
            log.info("Rolled back every action because an error occurred when making a return request");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Error occurred when making the return request. Please try again later");
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
                            .userId(borrow.getUserId())
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



        String token = getAuthToken();
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        // Get the user that is making the request
        User requester = getUserInfo(entity);

        if (request.getUserIdToTransfer() == requester.getUserId())
            throw new TransferMapException("You cannot transfer maps to yourself!");

        Borrows borrow = getBorrowById(request.getBorrowId());

        // Ownership check (redundant but safe)
        if (borrow.getUserId() != requester.getUserId()) {
            throw new AccessDeniedException("You do not own this borrowed map");
        }

        // Get the user the map is transferred to (check if he is enabled)
        User userToTransfer = checkIfUserIsEnabledByUserId(entity, request.getUserIdToTransfer());

        // Get the map info
        Map mapToTransfer = getMapInfo(request.getMapId(), entity);

        if (!mapToTransfer.canBeTransferred()) {
            throw new InvalidMapException("Map cannot be transferred!");
        }

        // Update the status of the old borrow
        borrow.setStatus(BorrowStatus.TRANSFERRED.dbValue());
        borrowsRepository.save(borrow);

        // create the new borrow
        Borrows newBorrow = Borrows.builder()
                .borrowDate(LocalDateTime.now())
                .userId(userToTransfer.getUserId())
                .mapId(mapToTransfer.getId())
                .returnDate(borrow.getReturnDate())
                .build();

        // save the enw new borrow
        borrowsRepository.save(newBorrow);

        return TransferResponse.builder()
                .newBorrow(newBorrow)
                .userTransferred(userToTransfer)
                .mapTransferred(mapToTransfer)
                .build();
    }

    private User getUserInfo(HttpEntity<Void> entity) {
        log.info("Requesting the user info from the identity-service");

        // requesting the client
        ResponseEntity<User> userResponse = restTemplate
                .exchange(
                        HTTP + userService + "/api/home",
                        HttpMethod.GET,
                        entity,
                        User.class
                );

        if (userResponse.getStatusCode().isSameCodeAs(HttpStatus.NOT_FOUND))
            throw new UserNotFoundException("User not found");

        if (userResponse.getBody() == null)
            throw new UserServiceException("User service response body is empty");

        log.info("The user response who borrows from the identity-service: {}", userResponse);
        return userResponse.getBody();

    }

    private Borrows getBorrowById(int borrowId) {
        return borrowsRepository.findById(borrowId)
                .orElseThrow(() -> new BorrowNotFoundException("Borrow not found"));
    }

    private User checkIfUserIsEnabledByUserId(HttpEntity<Void> entity, int userId) {
        log.info("Checking if the user is enabled from identity-service");

        // requesting the client
        ResponseEntity<User> userResponse = restTemplate
                .exchange(
                        HTTP + userService + "/api/users/{userId}",
                        HttpMethod.GET,
                        entity,
                        User.class,
                        userId
                );

        if (userResponse.getStatusCode().isSameCodeAs(HttpStatus.NOT_FOUND))
            throw new UserNotFoundException("User not found");

        if (userResponse.getBody() == null)
            throw new UserServiceException("User service response body is empty");

        log.info("The user checked by the identity-service: {}", userResponse);
        return userResponse.getBody();
    }

}
