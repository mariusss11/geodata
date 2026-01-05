package com.geodata.controllers;

import com.geodata.services.BorrowsService;
import com.geodata.utils.*;
import com.geodata.utils.requests.BorrowMapRequest;
import com.geodata.utils.requests.ReturnItemRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/borrows")
public class UserBorrowController {

    private final BorrowsService borrowsService;

    @GetMapping("/{clientId}")
    public ResponseEntity<List<Integer>> getClientBorrows(@PathVariable int clientId) {
        return ResponseEntity.ok(borrowsService.getClientBorrowedItems(clientId));
    }

    @PostMapping("/create")
    public ResponseEntity<String> makeRequestToBorrowMap(@RequestBody BorrowMapRequest request) {
        log.info("Trying to create a borrow request with: {}", request);
        return borrowsService.makeRequestToBorrowMap(request);
    }

//    @GetMapping("/returned-items/{clientId}")
//    public ResponseEntity<List<BorrowActions>> getClientReturnedItems(@PathVariable int clientId) {
//        return ResponseEntity.ok(borrowsService.getClientReturnedItems(clientId));
//    }

//    @GetMapping("/returned-items/filtered")
//    public ResponseEntity<List<BorrowActionsDTO>> getClientReturnedItemsByQuery(
//            @RequestParam int clientId,
//            @RequestParam String query
//    ) {
//        return ResponseEntity.ok(borrowsService.getClientReturnedItemsFiltered(clientId, query));
//    }

    @GetMapping("/hasBorrowed/{clientId}/{itemId}")
    public ResponseEntity<Boolean> getIfClientHadEverBorrowedTheItem(
            @PathVariable int clientId,
            @PathVariable int itemId
    ) {
        return ResponseEntity.ok(borrowsService.hadBorrowedItem(clientId, itemId));
    }

    /**
     * @param clientId use to get the client info
     * @return the ids of the items the client is currently borrowing, status <b>borrowed</b>
     */
    @GetMapping("/current/{clientId}")
    public ResponseEntity<List<Integer>> getClientCurrentBorrows(@PathVariable int clientId) {
        log.info("Returning items that are currently borrowed by the client");
        return ResponseEntity.ok(borrowsService.getClientAllCurrentBorrowsById(clientId));
    }

    /**
     * @param clientId use to get the client info
     * @return the ids of the items the client is currently borrowing, status <b>borrowed</b>
     */
    @GetMapping("/currently-borrowing/filtered")
    public ResponseEntity<List<BorrowsDTO>> getClientItemsCurrentlyBorrowed(
            @RequestParam int clientId,
            @RequestParam(name = "searchQuery") String query
    ) {
        log.info("Returning the items that are currently borrowed by the client");
        return ResponseEntity.ok(borrowsService.getClientMapsCurrentlyBorrowed(clientId, query));
    }

//    /**
//     * @param clientId use to get the client info
//     * @return the items the client is currently borrowing, status <b>borrowed</b>
//     */
//    @GetMapping("/current/{clientId}")
//    public ResponseEntity<List<Integer>> getClientCurrentBorrows(@PathVariable int clientId, @RequestBody List<Integer> itemList) {
//        return ResponseEntity.ok(borrowsService.getClientAllCurrentBorrowsById(clientId));
//    }



    @PostMapping("/return")
    public ResponseEntity<String> returnBorrowedItem(@RequestBody ReturnItemRequest request) {
        return borrowsService.makeRequestToReturnItem(request);
    }
}
