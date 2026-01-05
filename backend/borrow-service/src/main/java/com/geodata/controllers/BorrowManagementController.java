package com.geodata.controllers;

import com.geodata.services.BorrowManagementService;
import com.geodata.utils.BorrowsDTO;
import com.geodata.utils.LibrarianReason;
import jakarta.validation.constraints.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RequestMapping("/api/borrows/librarian")
@RestController
public class BorrowManagementController {

    private final BorrowManagementService borrowManagementService;

    @Autowired
    public BorrowManagementController(BorrowManagementService borrowManagementService) {
        this.borrowManagementService = borrowManagementService;
    }

    @GetMapping("/borrowRequests/number")
    public ResponseEntity<Integer> getBorrowRequestsNumber() {
        return ResponseEntity.ok(borrowManagementService.getBorrowRequestsNumber());
    }

    @GetMapping("/returnRequests/number")
    public ResponseEntity<Integer> getReturnRequestsNumber() {
        return ResponseEntity.ok(borrowManagementService.getReturnRequestsNumber());
    }

//    @GetMapping("/allBorrowActions/today")
//    public ResponseEntity<List<BorrowActions>> getAllBorrowActions() {
//        return ResponseEntity.ok(borrowManagementService.getAllTodayBorrowActions());
//    }

    @GetMapping("/all")
    public ResponseEntity<List<BorrowsDTO>> getAllBorrows() {
        return ResponseEntity.ok(borrowManagementService.getAllBorrows());
    }

//    @GetMapping("/borrow-history")
//    public ResponseEntity<List<BorrowActionsDTO>> getBorrowHistory() {
//        return ResponseEntity.ok(borrowManagementService.getBorrowHistory());
//    }


    @PostMapping("/approveBorrow/{clientId}/{itemId}")
    public ResponseEntity<String> approveBorrow(
            @NotNull @PathVariable int clientId,
            @NotNull @PathVariable int itemId,
            @RequestBody(required = false) LibrarianReason reason
    ) {
        return borrowManagementService.approveBorrowByLibrarian(clientId, itemId, reason);
    }

    @PostMapping("/declineBorrow/{clientId}/{itemId}")
    public ResponseEntity<Object> declineBorrow(
            @NotNull @PathVariable int clientId,
            @NotNull @PathVariable int itemId,
            @RequestBody(required = false) LibrarianReason reason
    ) {
        return ResponseEntity.ok(borrowManagementService.declineBorrowByLibrarian(clientId, itemId, reason));
    }

    @PostMapping("/approveReturn/{clientId}/{itemId}")
    public ResponseEntity<Object> approveReturn(
            @NotNull @PathVariable int clientId,
            @NotNull @PathVariable int itemId,
            @RequestBody(required = false) LibrarianReason reason
    ) {
        return ResponseEntity.ok(borrowManagementService.approveReturnByLibrarian(clientId, itemId, reason));
    }

    @PostMapping("/declineReturn/{clientId}/{itemId}")
    public ResponseEntity<Object> declineReturn(
            @NotNull @PathVariable int clientId,
            @NotNull @PathVariable int itemId,
            @RequestBody(required = false) LibrarianReason reason
    ) {
        return ResponseEntity.ok(borrowManagementService.declineReturnByLibrarian(clientId, itemId, reason));
    }

}
