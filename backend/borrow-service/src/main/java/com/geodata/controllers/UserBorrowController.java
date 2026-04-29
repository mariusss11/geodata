package com.geodata.controllers;

import com.geodata.model.Map;
import com.geodata.model.PagedResponse;
import com.geodata.services.BorrowsService;
import com.geodata.utils.BorrowedMapDto;
import com.geodata.utils.TransferMapRequest;
import com.geodata.utils.TransferResponse;
import com.geodata.utils.requests.BorrowMapRequest;
import com.geodata.utils.requests.ReturnMapRequest;
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
        return borrowsService.borrowMap(request);
    }

    @PostMapping("/return")
    public ResponseEntity<String> transferBorrowedItem(@RequestBody ReturnMapRequest request) {
        return borrowsService.returnMap(request);
    }

    @PostMapping("/transfer")
    public ResponseEntity<TransferResponse> transferBorrowedItem(@RequestBody TransferMapRequest request) {
        return ResponseEntity.ok(borrowsService.transferBorrowedItem(request));
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
     * @return the ids of the items the client is currently borrowing, status <b>borrowed</b>
     */
    @GetMapping("/current")
    public ResponseEntity<PagedResponse<BorrowedMapDto>> getUserCurrentBorrows(
            @RequestParam int pageNumber,
            @RequestParam int pageSize,
            @RequestParam(required = false) String searchQuery
    ) {
        log.info("Returning items that are currently borrowed by the client");
        return ResponseEntity.ok(borrowsService.getUserCurrentBorrows(pageNumber, pageSize, searchQuery));
    }

//    @GetMapping("/currently-borrowing/filtered")
//    public ResponseEntity<List<BorrowsDTO>> getClientItemsCurrentlyBorrowed(
//            @RequestParam int clientId,
//            @RequestParam(name = "searchQuery") String query
//    ) {
//        log.info("Returning the items that are currently borrowed by the client");
//        return ResponseEntity.ok(borrowsService.getClientMapsCurrentlyBorrowed(clientId, query));
//    }

//    /**
//     * @param clientId use to get the client info
//     * @return the items the client is currently borrowing, status <b>borrowed</b>
//     */
//    @GetMapping("/current/{clientId}")
//    public ResponseEntity<List<Integer>> getClientCurrentBorrows(@PathVariable int clientId, @RequestBody List<Integer> itemList) {
//        return ResponseEntity.ok(borrowsService.getClientAllCurrentBorrowsById(clientId));
//    }




}
