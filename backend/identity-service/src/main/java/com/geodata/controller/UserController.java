package com.geodata.controller;

import com.geodata.model.User;
import com.geodata.service.UserService;
import com.geodata.utils.PagedResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/users")

public class UserController {

    private final UserService userService;

    @Autowired
    public UserController(UserService userService) {
        this.userService = userService;
    }

   @GetMapping("/{userId}")
    public ResponseEntity<User> getUsersPaginated(
            @PathVariable int userId
   ) {
        return ResponseEntity.ok(userService.getUserById(userId));
   }

    @GetMapping("/paginated")
    public ResponseEntity<PagedResponse<User>> getUsersPaginated(
            @RequestParam int pageNumber,
            @RequestParam int pageSize,
            @RequestParam(required = false) String search
    ) {
        return ResponseEntity.ok(userService.getUsersPaginated(pageNumber, pageSize, search));
    }

    @GetMapping("/paginated/exclude-me")
    public ResponseEntity<PagedResponse<User>> getUsersPaginatedExcludeCurrent(
            @RequestParam int pageNumber,
            @RequestParam int pageSize,
            @RequestParam(required = false) String search
    ) {
        return ResponseEntity.ok(
                userService.getUsersPaginatedExcludeUser(pageNumber, pageSize, search)
        );
    }





}
