package com.geodata.controller;

import com.geodata.dto.ChangePasswordRequest;
import com.geodata.dto.Response;
import com.geodata.dto.UpdateProfileRequest;
import com.geodata.model.User;
import com.geodata.service.UserService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RequestMapping("/api/home")
@RestController
public class HomeController {

    private final UserService userService;

    @Autowired
    public HomeController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping()
    public ResponseEntity<User> getUserInfo() {
        return ResponseEntity.ok(userService.getUserInfo());
    }

    @GetMapping("/whoami")
    public ResponseEntity<Response<User>> whoami() {
        log.info("In the whoami method ");
        return ResponseEntity.ok(userService.whoami());
    }

    @PutMapping("/profile")
    public ResponseEntity<User> updateProfile(@Valid @RequestBody UpdateProfileRequest request) {
        return ResponseEntity.ok(userService.updateProfile(request));
    }

    @PutMapping("/password")
    public ResponseEntity<Void> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        userService.changePassword(request);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/disable")
    public ResponseEntity<String> disableUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        log.info("Disabling the user with the email: {}", email);
        return ResponseEntity.ok(userService.disableUser(email));
    }

}
