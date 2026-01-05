package com.geodata.controller;

import com.geodata.dto.LoginRequest;
import com.geodata.dto.CreateUserRequest;
import com.geodata.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/auth")
public class UserController {

    private final UserService userService;

    @Autowired
    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/register")
    public ResponseEntity<String> signUp(@Valid @RequestBody CreateUserRequest createUserRequest){
        log.info("CreateUserRequest request: {}", createUserRequest);
        return userService.signUp(createUserRequest);
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest loginRequest, HttpServletRequest request){
        String requestURL = request.getRequestURL().toString(); // Full URL without query params
        String queryString = request.getQueryString();          // Raw query string (can be null)
        String fullURL = requestURL + (queryString != null ? "?" + queryString : "");
        log.info("The full URL: {}", fullURL);

        return userService.login(loginRequest);
    }





}
