package com.geodata.service;

import com.geodata.dto.ChangePasswordRequest;
import com.geodata.dto.LoginRequest;
import com.geodata.dto.Response;
import com.geodata.dto.CreateUserRequest;
import com.geodata.dto.UpdateProfileRequest;
import com.geodata.model.User;
import com.geodata.utils.PagedResponse;
import org.springframework.http.ResponseEntity;

public interface UserService {

    ResponseEntity<String> signUp(CreateUserRequest createUserRequest);
    ResponseEntity<?> login(LoginRequest userRequest);

    User getCurrentLoggedInUser();

    Response<User> whoami();

    String disableUser(String email);

    User getUserInfo();

    User getUserById(int userId);

    User updateProfile(UpdateProfileRequest request);

    void changePassword(ChangePasswordRequest request);

    PagedResponse<User> getUsersPaginated(int pageNumber, int pageSize, String search);

    PagedResponse<User> getUsersPaginatedExcludeUser(int pageNumber, int pageSize, String search);
}