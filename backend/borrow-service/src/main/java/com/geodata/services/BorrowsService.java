package com.geodata.services;

import com.geodata.exceptions.*;
import com.geodata.model.Borrows;
import com.geodata.model.BorrowsStatus;
import com.geodata.repository.BorrowsRepository;
import com.geodata.security.jwt.JwtUtils;
import com.geodata.utils.*;
import com.geodata.utils.requests.BorrowMapRequest;
import com.geodata.utils.requests.LockItemRequest;
import com.geodata.utils.requests.ReturnItemRequest;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

// https://www.baeldung.com/spring-rest-template-error-handling
// https://howtodoinjava.com/spring-boot2/resttemplate/spring-restful-client-resttemplate-example/
@Slf4j
@Service
public class BorrowsService {

    @Value("${services.clientService}")
    private String clientService;

    @Value("${services.itemService}")
    private String itemService;


    private static final String HTTP = "http://";

    private final BorrowsRepository borrowsRepository;
    private final RestTemplate restTemplate;
    private final JwtUtils jwtUtils;
    private final HttpServletRequest httpRequest;


    @Autowired
    public BorrowsService(BorrowsRepository borrowsRepository, RestTemplate restTemplate, JwtUtils jwtUtils, HttpServletRequest httpRequest) {
        this.borrowsRepository = borrowsRepository;
        this.restTemplate = restTemplate;
        this.jwtUtils = jwtUtils;
        this.httpRequest = httpRequest;
    }

    public ResponseEntity<String> makeRequestToBorrowMap(BorrowMapRequest request) {
        log.info("Borrowing item: {}", request);

        String token = getAuthToken();
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        HttpEntity<Void> entity = new HttpEntity<>(headers);


        // defining the object that will be used
        LibraryItem itemToBorrow= null;
        Client clientThatBorrows;
        Borrows borrowRecord = null;

        try {

            log.info("➡️ Calling RestTemplate with title={}, itemType={}, author={}",
                    request.getItemTitle(), request.getItemType(), request.getAuthorName());

            // requesting the item to borrow
            ResponseEntity<LibraryItem> itemResponse = restTemplate
                    .exchange(
                            HTTP + itemService + clientService + "/api/items/params?title={title}&itemType={itemType}&authorName={authorName}",
                            HttpMethod.GET,
                            entity,
                            LibraryItem.class,
                            request.getItemTitle(),
                            request.getItemType(),
                            request.getAuthorName()
                    );

            if (itemResponse.getBody() == null)
                throw new ItemServiceException("Item service response is empty");

            itemToBorrow = itemResponse.getBody();
            if (!itemToBorrow.isAvailable() || !itemToBorrow.isEnabled())
                throw new ItemNotAvailableException("Item not available");


            // requesting the client
            ResponseEntity<Client> clientResponse = restTemplate
                    .exchange(
                            HTTP + clientService + clientService + "/api/client",
                            HttpMethod.GET,
                            entity,
                            Client.class
                    );

            if (clientResponse.getStatusCode().isSameCodeAs(HttpStatus.NOT_FOUND))
                throw new ClientNotFoundException("Client not found");


            if (clientResponse.getBody() == null)
                throw new ClientServiceException("Client service response body is empty");

            log.info("The client response who borrows: {}", clientResponse);
            clientThatBorrows = clientResponse.getBody();
            log.info("The client that borrows: {}", clientThatBorrows);


            // START MAKING CHANGES
            // lock the item (availability set to PENDING_APPROVAL)
            HttpHeaders postHeaders = new HttpHeaders();
            postHeaders.setContentType(MediaType.APPLICATION_JSON);
            postHeaders.setBearerAuth(token);

            LockItemRequest lockRequest = new LockItemRequest(itemToBorrow.getId());
            HttpEntity<LockItemRequest> lockEntity = new HttpEntity<>(lockRequest, postHeaders);

            ResponseEntity<Void> updateResponse = restTemplate.exchange(
                    HTTP + itemService + clientService + "/api/items/setAvailableToPending",
                    HttpMethod.PUT,
                    lockEntity,
                    Void.class
            );

            if (updateResponse.getStatusCode().isError())
                throw new ItemServiceException("Error occurred when changing the status on the item");

            // STEP 4: Save borrow record
            borrowRecord = borrowsRepository.save(
                    Borrows.builder()
                            .userId(clientThatBorrows.getClientId())
                            .mapId(itemToBorrow.getId())
                            .status(BorrowsStatus.BORROW_REQUESTED.dbValue())
                            .build()
            );

            log.info("Borrow saga completed successfully. Borrow record create: {}", borrowRecord);

            // Catching
            // checking if the borrow failed because the item is already borrowed,
            // or the client is not found
        } catch (ItemServiceException | ClientServiceException | ItemNotAvailableException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (Exception e) {

            log.info("Saga failed: {}", e.getMessage());

            // COMPENSATIONS STEP 1: Unlock item if locked (set the availability to available
            try {
                HttpHeaders unlockHeaders = new HttpHeaders();
                unlockHeaders.setContentType(MediaType.APPLICATION_JSON);
                unlockHeaders.setBearerAuth(token);

                assert itemToBorrow != null;
                LockItemRequest unlockRequest = new LockItemRequest(itemToBorrow.getId());
                HttpEntity<LockItemRequest> unlockEntity = new HttpEntity<>(unlockRequest, unlockHeaders);

                restTemplate.exchange(
                        HTTP + itemService + clientService + "/api/items/setPendingToAvailable",
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
        return ResponseEntity.ok("Borrow request sent successfully");
    }

    public ResponseEntity<String> makeRequestToReturnItem(ReturnItemRequest request) {
        log.info("Returning item: {}", request);

        String authHeader = httpRequest.getHeader("Authorization");
        String token = jwtUtils.getToken(authHeader);

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        LibraryItem itemToReturn = null;
        Client clientThatReturns = null;
        Borrows borrowRecord = null;

        try {

            // request the item
            ResponseEntity<LibraryItem> itemResponse = restTemplate.exchange(
                    HTTP + itemService + clientService + "/api/items/params?title={title}&itemType={itemType}&authorName={authorName}",
                    HttpMethod.GET,
                    entity,
                    LibraryItem.class,
                    request.getItemTitle(),
                    request.getItemType(),
                    request.getAuthorName()
            );

            if (itemResponse.getBody() == null)
                throw new ItemServiceException("Item service response is empty");

            itemToReturn = itemResponse.getBody();

            if (itemToReturn.isAvailable()) {
                log.info("The item is not borrowed");
                throw new ItemNotBorrowedException("Item is not borrowed: " + request.getItemTitle());
            }

            // request the client
            ResponseEntity<Client> clientResponse = restTemplate.exchange(
                    HTTP + clientService + clientService + "/api/client",
                    HttpMethod.GET,
                    entity,
                    Client.class
            );

            if (clientResponse.getBody() == null)
                throw new ClientServiceException("Client service response is empty");

            clientThatReturns = clientResponse.getBody();

            // check if this client had borrowed the item
            borrowRecord = borrowsRepository.findByUserIdAndMapIdAndStatus(
                            clientThatReturns.getClientId(),
                            itemToReturn.getId(),
                            BorrowsStatus.BORROWED.dbValue()).stream().findFirst()
                    .orElseThrow(() -> new RuntimeException("You haven't borrowed this item"));

            // start making some changes
            // unlock the item (set the status to pending)
            HttpHeaders putHeaders = new HttpHeaders();
            putHeaders.setContentType(MediaType.APPLICATION_JSON);
            putHeaders.setBearerAuth(token);

            LockItemRequest lockRequest = new LockItemRequest(itemToReturn.getId());
            HttpEntity<LockItemRequest> unLockEntity = new HttpEntity<>(lockRequest, putHeaders);

            try {
                ResponseEntity<Void> updateResponse = restTemplate.exchange(
                        HTTP + itemService + clientService + "/api/items/setBorrowedToPending",
                        HttpMethod.PUT,
                        unLockEntity,
                        Void.class
                );
                log.info("Status changed successfully after requesting returning of the item: {}", updateResponse.getStatusCode());
            } catch (Exception e) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body("Error occurred when changing the status of hte item during request to return an item");
            }

            // update the borrow record
            borrowRecord.setStatus(BorrowsStatus.RETURN_REQUESTED.dbValue());
            borrowsRepository.save(borrowRecord);

            log.info("Return saga completed successfully");

        } catch (ItemServiceException | ClientServiceException | ItemNotAvailableException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (Exception e) {
            log.error("Exception during returnItem, starting compensation: {}", e.getMessage());

            // Compensation steps
            // Trying to change the status back to how it was
            try {
                HttpHeaders lockHeaders = new HttpHeaders();
                lockHeaders.setContentType(MediaType.APPLICATION_JSON);
                lockHeaders.setBearerAuth(token);

                LockItemRequest lockRequest = new LockItemRequest(itemToReturn.getId());
                HttpEntity<LockItemRequest> lockEntity = new HttpEntity<>(lockRequest, lockHeaders);

                restTemplate.exchange(
                        HTTP + itemService + clientService + "/api/items/setPendingToBorrowed",
                        HttpMethod.PUT,
                        lockEntity,
                        Void.class
                );
                log.info("Compensation: locked the item again");
            } catch (Exception lockEx) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error occurred when changing the status for compensation when returning item");
            }
            log.info("Rolled back every action because an error occurred when making a return request");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Error occurred when making a return request");
        }
        return ResponseEntity.ok("Return request sent successfully");
    }

    public long removeItemByIdFromBorrowsList(int itemId) {
        log.info("Removing the item from the borrowed list who's id is: {}", itemId);
        return borrowsRepository.deleteByItemId(itemId);
    }

    public List<Integer> getClientBorrowedItems(int clientId) {
        log.info("Trying to return client borrowed items");
        List<Borrows> borrowsList = borrowsRepository.findAllByClientIdAndStatus(clientId, BorrowsStatus.RETURNED.dbValue());
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
     * @param clientId the ID of the client whose current borrowed items are to be retrieved
     * @return a list of item IDs that the specified client has currently borrowed and not yet returned
     */
    public List<Integer> getClientAllCurrentBorrowsById(int clientId) {
        // get the list of the current borrow that is not returned
        List<Borrows> borrowItems = borrowsRepository.findBorrowItemsByClientIdAndStatus(clientId, BorrowsStatus.BORROWED.dbValue());

        // return the list of the item ids
        return borrowItems.stream().map(Borrows::getMapId).toList();
    }

    public List<BorrowsDTO> getClientMapsCurrentlyBorrowed(int clientId, String query) {
        log.info("Getting client items that are currently being borrowed with query: {}", query);
        List<LibraryItem> itemsByQuery = getItemsByQuery(query);

        // Create a lookup map: itemId -> LibraryItem
        Map<Integer, LibraryItem> itemMap = itemsByQuery.stream()
                .collect(Collectors.toMap(LibraryItem::getId, item -> item));

        // The item IDs
        List<Integer> itemIds = new ArrayList<>(itemMap.keySet());

        List<Borrows> borrowsList = borrowsRepository.findBorrowsCurrentlyBorrowedByClientFiltered(clientId, itemIds);

        List<BorrowsDTO> result = new ArrayList<>();
        for (Borrows borrow : borrowsList) {
            LibraryItem item = itemMap.get(borrow.getMapId());
            BorrowsDTO dto = BorrowMapper.toDto(borrow, item);
            result.add(dto);
        }

        log.info("Returning client's items that are currently being borrowed");
        // return the list of the item ids
        return result;
    }

    /**
     * We send the list of items that were seen in the query in the frontend,
     * and now the backend just sees which of the items are borrowed by the client
     */
    public List<Integer> getClientAllCurrentBorrowsByIdAndItemsIdList(int clientId, List<Integer> itemIdList) {
        // get the list of the current borrow that is not returned
        List<Borrows> borrowItems = borrowsRepository.findBorrowItemsByClientIdAndStatus(clientId, BorrowsStatus.BORROWED.dbValue());

        // return the list of the item ids
        return borrowItems.stream().map(Borrows::getMapId).toList();
    }

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
                    HTTP + itemService + clientService + "/api/items/setBorrowedToPending",
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
                HTTP + itemService + "/api/items/search?query={query}",
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
}
