package com.geodata.service;

import com.geodata.dto.LoginRequest;
import com.geodata.dto.Response;
import com.geodata.dto.CreateUserRequest;
import com.geodata.model.User;
import org.springframework.http.ResponseEntity;

public interface UserService {

    ResponseEntity<String> signUp(CreateUserRequest createUserRequest);
    ResponseEntity<?> login(LoginRequest userRequest);

    User getCurrentLoggedInUser();

    Response<User> whoami();

    String disableUser(String email);

}