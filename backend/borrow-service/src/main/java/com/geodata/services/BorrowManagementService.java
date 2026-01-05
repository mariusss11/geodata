package com.geodata.services;

import com.geodata.exceptions.ClientServiceException;
import com.geodata.exceptions.ItemServiceException;
import com.geodata.model.Borrows;
import com.geodata.model.BorrowsStatus;
import com.geodata.repository.BorrowsRepository;
import com.geodata.security.jwt.JwtUtils;
import com.geodata.utils.*;
import com.geodata.utils.requests.LockItemRequest;
import jakarta.annotation.Nullable;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class BorrowManagementService {

    @Value("${services.clientService}")
    private String clientService;

    @Value("${services.itemService}")
    private String itemService;

    private static final String HTTP = "http://";

    private final BorrowsRepository borrowsRepository;
    private final JwtUtils jwtUtils;
    private final RestTemplate restTemplate;
    private final HttpServletRequest httpRequest;

    private static final String NO_REASON_PROVIDED_MESSAGE = "No reason provided";

    @Autowired
    public BorrowManagementService(BorrowsRepository borrowsRepository, JwtUtils jwtUtils, RestTemplate restTemplate, HttpServletRequest httpRequest) {
        this.borrowsRepository = borrowsRepository;
        this.jwtUtils = jwtUtils;
        this.restTemplate = restTemplate;
        this.httpRequest = httpRequest;
    }


    public List<BorrowsDTO> getAllBorrows() {
        List<Borrows> allBorrows = borrowsRepository.findAll();
        List<BorrowsDTO> borrowsDTOList = new ArrayList<>();


        HttpHeaders getHeaders = new HttpHeaders();
        getHeaders.setContentType(MediaType.APPLICATION_JSON);
        getHeaders.setBearerAuth(getAuthToken());
        log.info("The auth token: {}", getAuthToken());

        HttpEntity<Void> entity = new HttpEntity<>(getHeaders);


        for (Borrows borrow : allBorrows) {
            // fetch the clients
            ResponseEntity<Client> clientServiceResponse = restTemplate.exchange(
                    HTTP + clientService + ":8010/api/client/librarian/{clientId}",
                    HttpMethod.GET,
                    entity,
                    Client.class,
                    borrow.getUserId()
            );

            if (clientServiceResponse.getBody() == null)
                throw new ClientServiceException("Client service response is empty");

            ResponseEntity<LibraryItem> itemServiceResponse = restTemplate.exchange(
                    HTTP + itemService + ":8020/api/items/{itemId}",
                    HttpMethod.GET,
                    entity,
                    LibraryItem.class,
                    borrow.getMapId()
            );

            // check if the response is valid
            if (itemServiceResponse.getBody() == null)
                throw new ItemServiceException("Item service response is empty");

            borrowsDTOList.add(
                    BorrowMapper.toDto(borrow, itemServiceResponse.getBody(), clientServiceResponse.getBody())
            );
        }
        return borrowsDTOList;
    }

    /**
     * When a librarian approves a borrow request,
     * it should be updated in the borrow table as <b>BORROWED</b>,
     * and added in the table <I>borrow_action</I> table that keeps all this records about borrowActions
     * @param clientId to identify the client whose request was approved
     * @param itemId to identify what item the client borrowed
     * @param reason <b>OPTIONAL</b> the reason the librarian approved the request
     * @return if everything works fine, a confirmation message, else an operation failed message
     */
    @Transactional
    public ResponseEntity<String> approveBorrowByLibrarian(@NotNull int clientId, @NotNull int itemId, @Nullable LibrarianReason reason) {
        // see if the borrow is in the db
        Borrows borrowToAccept = getBorrowByItemIdClientIdAndStatus(clientId, itemId, BorrowsStatus.BORROW_REQUESTED);

        String reasonText = getReasonMessage(reason);

        // update the availability on the item
        HttpHeaders putHeaders = new HttpHeaders();
        putHeaders.setContentType(MediaType.APPLICATION_JSON);
        putHeaders.setBearerAuth(getAuthToken());

        LockItemRequest setPendingToBorrowedRequest = new LockItemRequest(itemId);
        HttpEntity<LockItemRequest> setPendingToBorrowedBody = new HttpEntity<>(setPendingToBorrowedRequest, putHeaders);

        try {
            ResponseEntity<Void> updateStatusResponse = restTemplate.exchange(
                    "http://localhost:8020/api/items/setPendingToBorrowed",
                    HttpMethod.PUT,
                    setPendingToBorrowedBody,
                    Void.class
            );
            log.info("Status change successfully after approval: {}", updateStatusResponse.getStatusCode());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Error occurred when changing the status of the item during borrow approval");
        }

        // update the status of the borrow
        borrowToAccept.setStatus(BorrowsStatus.BORROWED.dbValue());
        borrowToAccept.setBorrowDate(LocalDateTime.now());
        // set the librarian id who accepted this borrow

        // save the updated borrow
        borrowsRepository.save(borrowToAccept);
        return ResponseEntity.ok("The borrow of the item was approved successfully");
    }

    /**
     * When a librarian declines a borrow request, it should be deleted from the borrow table,
     * but added in one special table that keeps all this records about borrowActions
     * @param clientId to identify the client whose request was rejected
     * @param itemId to identify what item the client wanted to borrow
     * @param reason the reason the librarian declined the request (e.g., 'Bad' client)
     * @return if everything works fine, a confirmation message, else an operation failed message
     */
    @Transactional
    public ResponseEntity<String> declineBorrowByLibrarian(@NotNull int clientId, @NotNull int itemId, LibrarianReason reason) {
        log.info("Declining the request to borrow and item");
        // see if the borrow is in the db
        Borrows borrowToDelete =
                getBorrowByItemIdClientIdAndStatus(clientId, itemId, BorrowsStatus.BORROW_REQUESTED);

        String reasonText = getReasonMessage(reason);

        // make the request to the item to change the item status
        HttpHeaders putHeaders = new HttpHeaders();
        putHeaders.setContentType(MediaType.APPLICATION_JSON);
        putHeaders.setBearerAuth(getAuthToken());

        LockItemRequest setPendingToBorrowedRequest = new LockItemRequest(itemId);
        HttpEntity<LockItemRequest> setPendingToBorrowedBody = new HttpEntity<>(setPendingToBorrowedRequest, putHeaders);

        try {
            ResponseEntity<Void> updateStatusResponse = restTemplate.exchange(
                    "http://localhost:8020/api/items/setPendingToAvailable",
                    HttpMethod.PUT,
                    setPendingToBorrowedBody,
                    Void.class
            );
            log.info("Status change successfully after delcine: {}", updateStatusResponse.getStatusCode());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Error occurred when changing the status of the item during borrow declining");
        }

        // delete the borrow
        borrowsRepository.delete(borrowToDelete);

        return ResponseEntity.ok("The borrow of the item was declined successfully");
    }


    @Transactional
    public ResponseEntity<String> approveReturnByLibrarian(@NotNull int clientId, @NotNull int itemId, LibrarianReason reason) {
        // see if the borrow is in the db
        Borrows returnToApprove =
                getBorrowByItemIdClientIdAndStatus(clientId, itemId, BorrowsStatus.RETURN_REQUESTED);

        String reasonText = getReasonMessage(reason);

        // update the status of the item
        HttpHeaders putHeaders = new HttpHeaders();
        putHeaders.setBearerAuth(getAuthToken());

        LockItemRequest request = new LockItemRequest(itemId);
        HttpEntity<LockItemRequest> entityToUpdate = new HttpEntity<>(request, putHeaders);

        try {
            ResponseEntity<Void> updateResponse = restTemplate.exchange(
                    "http://localhost:8020/api/items/setPendingToAvailable",
                    HttpMethod.PUT,
                    entityToUpdate,
                    Void.class
            );
            log.info("Status changed successfully after return approval: {}", updateResponse.getStatusCode());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Error occurred when changing the status of the item during return approval");
        }

        // update the status in the db
        returnToApprove.setStatus(BorrowsStatus.RETURNED.dbValue());
        returnToApprove.setReturnDate(LocalDateTime.now());
        // set the librarian id who approve this borrow

        // save the updated borrow
        borrowsRepository.save(returnToApprove);
        return ResponseEntity.ok("The item return was approved successfully");
    }


    /**
     * When a librarian declines a request, the row in the borrow table is set back to borrowed,
     * because the item will remain borrowed,
     * and a new row will be added in the borrow_actions table
     * @param clientId to identify the client whose request was rejected
     * @param itemId to identify what item the client wanted to return
     * @param reason the reason the librarian declined the request
     * @return if everything works fine, a confirmation message, else an operation failed message
     */
    @Transactional
    public ResponseEntity<String> declineReturnByLibrarian(@NotNull int clientId, @NotNull int itemId, LibrarianReason reason) {
        // see if the borrow is in the db
        Borrows returnToDecline =
                getBorrowByItemIdClientIdAndStatus(clientId, itemId, BorrowsStatus.RETURN_REQUESTED);

        String reasonText = getReasonMessage(reason);

        // update the status of the item
        HttpHeaders putHeaders = new HttpHeaders();
        putHeaders.setBearerAuth(getAuthToken());

        LockItemRequest request = new LockItemRequest(itemId);
        HttpEntity<LockItemRequest> entityToUpdate = new HttpEntity<>(request, putHeaders);

        try {
            ResponseEntity<Void> updateStatusResponse = restTemplate.exchange(
                    "http://localhost:8020/api/items/setPendingToBorrowed",
                    HttpMethod.PUT,
                    entityToUpdate,
                    Void.class
            );
            log.info("Status change successfully after return decline: {}", updateStatusResponse.getStatusCode());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Error occurred when changing the status of the item during return declining");
        }

        // update the status in the db
        // it will be still borrowed because the item is still borrowed
        returnToDecline.setStatus(BorrowsStatus.BORROWED.dbValue());
        // set the librarian id who approve this borrow

        // save the updated borrow
        borrowsRepository.save(returnToDecline);
        return ResponseEntity.ok("The item return was declined successfully");
    }

    String getReasonMessage(LibrarianReason reason) {
        return (reason != null && reason.getReason() != null) ? reason.getReason() : NO_REASON_PROVIDED_MESSAGE;
    }

    private String getAuthToken() {
        return jwtUtils.getToken(httpRequest.getHeader("Authorization"));
    }

    /**
     * Method use to validate the borrow that should be approved or decline
     * @param clientId the client that was involved in the action
     * @param itemId the item that was involved
     * @param status the status the item has (e.g., BORROWED, RETURNED)
     * @return the borrow if it exists
     */
    private Borrows getBorrowByItemIdClientIdAndStatus(@NotNull int clientId, @NotNull int itemId, @NotNull BorrowsStatus status) {
        return borrowsRepository.findByUserIdAndMapIdAndStatus(clientId, itemId, status.dbValue()).stream().findFirst()
                .orElseThrow(() -> new RuntimeException("Borrow not found! Operation failed"));
    }

//    public List<BorrowActionsDTO> getBorrowHistory() {
//        List<BorrowActions> borrowHistory = borrowActionsService.getAllBorrowActions();
//        List<BorrowActionsDTO> borrowHistoryDTO = new ArrayList<>();
//
//        HttpHeaders getHeaders = new HttpHeaders();
//        getHeaders.setContentType(MediaType.APPLICATION_JSON);
//        getHeaders.setBearerAuth(getAuthToken());
//        HttpEntity<Void> entity = new HttpEntity<>(getHeaders);
//
//
//        for (BorrowActions borrowActions : borrowHistory) {
//            ResponseEntity<LibraryItem> itemServiceResponse = restTemplate.exchange(
//                    HTTP + itemService + ":8020/api/items/{itemId}",
//                    HttpMethod.GET,
//                    entity,
//                    LibraryItem.class,
//                    borrowActions.getItemId()
//            );
//
//            if (itemServiceResponse.getBody() == null)
//                throw new ItemServiceException("Item service response is empty");
//
//            ResponseEntity<Client> clientServiceResponse = restTemplate.exchange(
//                    HTTP + clientService + ":8010/api/client/librarian/{clientId}",
//                    HttpMethod.GET,
//                    entity,
//                    Client.class,
//                    borrowActions.getClientId()
//            );
//
//            if (clientServiceResponse.getBody() == null)
//                throw new ClientServiceException("Client service exception is empty");
//
//            borrowHistoryDTO.add(BorrowActionsMapper
//                    .toDto(borrowActions, itemServiceResponse.getBody(), clientServiceResponse.getBody())
//            );
//        }
//        return borrowHistoryDTO;
//    }

//    public List<BorrowActions> getAllTodayBorrowActions() {
//        return borrowActionsService.getAllTodayBorrowActions();
//    }

    public Integer getBorrowRequestsNumber() {
        return borrowsRepository.findAllByStatus(BorrowsStatus.BORROW_REQUESTED.dbValue()).size();
    }

    public Integer getReturnRequestsNumber() {
        return borrowsRepository.findAllByStatus(BorrowsStatus.RETURN_REQUESTED.dbValue()).size();
    }
}
